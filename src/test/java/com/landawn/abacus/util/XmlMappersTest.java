package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.landawn.abacus.TestBase;

import lombok.Data;

public class XmlMappersTest extends TestBase {

    @TempDir
    File tempDir;

    @Data
    public static class Person {
        private String name = "";
        private int age = 0;
        private String city = "";

        public Person() {
        }

        public Person(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public Person(final String name, final int age, final String city) {
            this.name = name;
            this.age = age;
            this.city = city;
        }
    }

    public static class SerializationVisibilityBean {
        private String value;

        public SerializationVisibilityBean() {
        }

        SerializationVisibilityBean(final String value) {
            this.value = value;
        }
    }

    public static class DeserializationVisibilityBean {
        private String value;

        public DeserializationVisibilityBean() {
        }

        String value() {
            return value;
        }
    }

    @JsonPropertyOrder({ "value", "missing" })
    public static class InclusionBean {
        public String value = "x";
        public String missing = null;

        public String getValue() {
            return value;
        }

        public String getMissing() {
            return missing;
        }
    }

    // Jackson caches a serializer per concrete type, so every entry point under test needs a type of its own
    // whose serializer is built after the mutation - a type already serialized keeps its old serializer.
    public static class InclusionBeanPlain extends InclusionBean {
    }

    public static class InclusionBeanPretty extends InclusionBean {
    }

    public static class InclusionBeanNullConfig extends InclusionBean {
    }

    public static class InclusionBeanFeature extends InclusionBean {
    }

    public static class InclusionBeanOwnConfig extends InclusionBean {
    }

    private static XmlMappers.One one() {
        return XmlMappers.wrap(new XmlMapper());
    }

    private File xmlFile(final String name, final Object value) throws IOException {
        final File file = new File(tempDir, name);
        XmlMappers.toXml(value, file);
        return file;
    }

    @Test
    public void testSerializationConfigDoesNotLeakCachedSerializer() {
        final XmlMapper fieldMapper = new XmlMapper();
        fieldMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        fieldMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        final String xml = XmlMappers.toXml(new SerializationVisibilityBean("secret"), fieldMapper.getSerializationConfig());
        assertTrue(xml.contains("<value>secret</value>"));

        final SerializationConfig defaultConfig = XmlMappers.createSerializationConfig();
        assertThrows(RuntimeException.class, () -> XmlMappers.toXml(new SerializationVisibilityBean("secret"), defaultConfig));
    }

    @Test
    public void testDeserializationConfigDoesNotLeakCachedRootDeserializer() {
        final XmlMapper fieldMapper = new XmlMapper();
        fieldMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        fieldMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        final String xml = "<DeserializationVisibilityBean><value>secret</value></DeserializationVisibilityBean>";

        final DeserializationVisibilityBean bean = XmlMappers.fromXml(xml, DeserializationVisibilityBean.class, fieldMapper.getDeserializationConfig());
        assertEquals("secret", bean.value());

        final DeserializationConfig defaultConfig = XmlMappers.createDeserializationConfig();
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(xml, DeserializationVisibilityBean.class, defaultConfig));
    }

    @Test
    public void testCreateSerializationConfigDoesNotShareMutableStateWithDefaultMapper() {
        final SerializationConfig mine = XmlMappers.createSerializationConfig();
        final SerializationConfig other = XmlMappers.createSerializationConfig();
        final JsonInclude.Value nonNull = JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL);

        try {
            // withPropertyInclusion(..) is the one Jackson config method that writes through the config's
            // ConfigOverrides in place and returns the receiver instead of a copy.
            assertSame(mine, mine.withPropertyInclusion(nonNull));

            // none of the entry points backed by this class's own mapper may see that mutation
            assertEquals("<InclusionBeanPlain><value>x</value><missing/></InclusionBeanPlain>", XmlMappers.toXml(new InclusionBeanPlain()));
            assertEquals("<InclusionBeanPretty><value>x</value><missing/></InclusionBeanPretty>",
                    XmlMappers.toXml(new InclusionBeanPretty(), true).replaceAll("\\s", ""));
            assertEquals("<InclusionBeanNullConfig><value>x</value><missing/></InclusionBeanNullConfig>",
                    XmlMappers.toXml(new InclusionBeanNullConfig(), (SerializationConfig) null));
            assertEquals("<InclusionBeanFeature><value>x</value><missing/></InclusionBeanFeature>",
                    XmlMappers.toXml(new InclusionBeanFeature(), SerializationFeature.WRITE_ENUMS_USING_TO_STRING));

            // nor may any other config handed out by the factories, before or after the mutation
            assertEquals(JsonInclude.Include.NON_NULL, mine.getDefaultPropertyInclusion().getValueInclusion());
            assertEquals(JsonInclude.Include.USE_DEFAULTS, other.getDefaultPropertyInclusion().getValueInclusion());
            assertEquals(JsonInclude.Include.USE_DEFAULTS, XmlMappers.createSerializationConfig().getDefaultPropertyInclusion().getValueInclusion());
            assertEquals(JsonInclude.Include.USE_DEFAULTS, XmlMappers.createDeserializationConfig().getDefaultPropertyInclusion().getValueInclusion());

            // the caller that asked for NON_NULL still gets exactly what it asked for
            assertEquals("<InclusionBeanOwnConfig><value>x</value></InclusionBeanOwnConfig>", XmlMappers.toXml(new InclusionBeanOwnConfig(), mine));
        } finally {
            // an unfixed build shares this object with the default mapper: put it back rather than leaving
            // the rest of the suite to run against a mutated default inclusion
            mine.withPropertyInclusion(JsonInclude.Value.empty());
        }
    }

    @Test
    public void testCreateConfigHandsOutSharedJacksonObjectsByReference() {
        // The flip side of the isolation above, and the reason createSerializationConfig()'s javadoc must not
        // promise that a returned config shares NO mutable state: its own ConfigOverrides is copied, but what it
        // hands out is not. Identity only - mutating any of these really would change toXml(..) for callers that
        // never touched this config, which is exactly why no test may do it.
        final SerializationConfig ser = XmlMappers.createSerializationConfig();
        final DeserializationConfig deser = XmlMappers.createDeserializationConfig();
        final XmlMapper untouched = new XmlMapper();

        // the pretty printer and the date format are Jackson's JVM-wide defaults
        assertSame(ser.getDefaultPrettyPrinter(), XmlMappers.createSerializationConfig().getDefaultPrettyPrinter());
        assertSame(ser.getDefaultPrettyPrinter(), untouched.getSerializationConfig().getDefaultPrettyPrinter());
        assertSame(ser.getDateFormat(), deser.getDateFormat());
        assertSame(ser.getDateFormat(), untouched.getSerializationConfig().getDateFormat());

        // the annotation introspector is NOT JVM-wide here: an XmlMapper builds its own XML-aware pair, so this
        // one is shared among this class's configurations but differs from a brand-new XmlMapper's
        assertSame(ser.getAnnotationIntrospector(), XmlMappers.createSerializationConfig().getAnnotationIntrospector());
        assertNotSame(ser.getAnnotationIntrospector(), untouched.getSerializationConfig().getAnnotationIntrospector());
        assertTrue(ser.getAnnotationIntrospector().allIntrospectors().containsAll(deser.getAnnotationIntrospector().allIntrospectors()));
    }

    @Test
    public void testToXml() {
        final Person person = new Person("John", 30);
        final String xml = XmlMappers.toXml(person);
        assertTrue(xml.contains("John"));
        assertTrue(xml.contains("30"));
        assertEquals(person, XmlMappers.fromXml(xml, Person.class));

        final Map<String, List<Person>> data = new HashMap<>();
        final List<Person> persons = new ArrayList<>();
        persons.add(new Person("Alice", 30, "NYC"));
        persons.add(new Person("Bob", 25, "LA"));
        data.put("people", persons);
        final String nested = XmlMappers.toXml(data);
        assertTrue(nested.contains("Alice"));
        assertTrue(nested.contains("Bob"));
    }

    @Test
    public void testToXml_PrettyFormat() {
        final Person person = new Person("Jane", 25);
        final String pretty = XmlMappers.toXml(person, true);
        final String compact = XmlMappers.toXml(person, false);
        assertTrue(pretty.contains("Jane"));
        assertTrue(pretty.contains("\n"));
        assertFalse(compact.contains("\n"));
    }

    @Test
    public void testToXml_SerializationFeatures() {
        final String xml = XmlMappers.toXml(new Person("Bob", 40), SerializationFeature.INDENT_OUTPUT);
        assertTrue(xml.contains("Bob"));
        assertTrue(xml.contains("40"));
    }

    @Test
    public void testToXml_SerializationConfig() {
        final SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
        final String xml = XmlMappers.toXml(new Person("Alice", 35), config);
        assertTrue(xml.contains("Alice"));
        assertTrue(XmlMappers.toXml(new Person("Test", 25), (SerializationConfig) null).contains("Test"));
    }

    @Test
    public void testToXml_File() throws IOException {
        final File file = new File(tempDir, "person.xml");
        XmlMappers.toXml(new Person("Charlie", 28), file);
        assertTrue(file.length() > 0);

        final File configured = new File(tempDir, "person-config.xml");
        XmlMappers.toXml(new Person("David", 33), configured, XmlMappers.createSerializationConfig());
        assertTrue(configured.length() > 0);
    }

    @Test
    public void testToXml_OutputStream() throws IOException {
        final boolean[] closed = { false };
        final ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        XmlMappers.toXml(new Person("AutoClose", 1), baos);
        assertTrue(closed[0]);
        assertTrue(baos.toString().contains("AutoClose"));

        final ByteArrayOutputStream configured = new ByteArrayOutputStream();
        XmlMappers.toXml(new Person("Frank", 45), configured, XmlMappers.createSerializationConfig());
        assertTrue(configured.toString().contains("Frank"));
    }

    @Test
    public void testToXml_Writer() throws IOException {
        final StringWriter writer = new StringWriter();
        XmlMappers.toXml(new Person("Grace", 29), writer);
        assertTrue(writer.toString().contains("Grace"));

        final StringWriter configured = new StringWriter();
        XmlMappers.toXml(new Person("Henry", 38), configured, XmlMappers.createSerializationConfig());
        assertTrue(configured.toString().contains("Henry"));
    }

    @Test
    public void testConfiguredOutputAutoCloseCanBeDisabled() {
        final SerializationConfig config = XmlMappers.createSerializationConfig().without(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        final boolean[] closed = { false, false };
        final ByteArrayOutputStream output = new ByteArrayOutputStream() {
            @Override
            public void close() {
                closed[0] = true;
            }
        };
        final StringWriter writer = new StringWriter() {
            @Override
            public void close() {
                closed[1] = true;
            }
        };

        final Person person = new Person("Still open", 17);
        XmlMappers.toXml(person, output, config);
        XmlMappers.toXml(person, writer, config);

        assertFalse(closed[0]);
        assertFalse(closed[1]);
        assertEquals(person, XmlMappers.fromXml(output.toByteArray(), Person.class));
        assertEquals(person, XmlMappers.fromXml(writer.toString(), Person.class));
    }

    @Test
    public void testConfiguredInputAutoCloseCanBeDisabled() {
        final DeserializationConfig config = XmlMappers.createDeserializationConfig().without(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        final Person person = new Person("Still open", 17);
        final String xml = XmlMappers.toXml(person);
        final TypeReference<Person> type = new TypeReference<>() {
        };

        for (final boolean generic : new boolean[] { false, true }) {
            final boolean[] closed = { false, false };
            final ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public void close() {
                    closed[0] = true;
                }
            };
            final StringReader reader = new StringReader(xml) {
                @Override
                public void close() {
                    closed[1] = true;
                }
            };

            assertEquals(person, generic ? XmlMappers.fromXml(input, type, config) : XmlMappers.fromXml(input, Person.class, config));
            assertEquals(person, generic ? XmlMappers.fromXml(reader, type, config) : XmlMappers.fromXml(reader, Person.class, config));
            assertFalse(closed[0]);
            assertFalse(closed[1]);
        }
    }

    @Test
    public void testToXml_DataOutput() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        XmlMappers.toXml(new Person("Ivy", 26), (DataOutput) new DataOutputStream(baos));
        assertTrue(baos.toString().contains("Ivy"));

        final ByteArrayOutputStream configured = new ByteArrayOutputStream();
        XmlMappers.toXml(new Person("Jack", 42), (DataOutput) new DataOutputStream(configured), XmlMappers.createSerializationConfig());
        assertTrue(configured.size() > 0);
    }

    @Test
    public void testFromXml() {
        final Person person = new Person("Eve", 27);
        final String xml = XmlMappers.toXml(person);
        assertEquals(person, XmlMappers.fromXml(xml, Person.class));
        assertEquals(person, XmlMappers.fromXml(xml.getBytes(StandardCharsets.UTF_8), Person.class));
        assertEquals(person, XmlMappers.fromXml(xml.getBytes(StandardCharsets.UTF_8), 0, xml.getBytes(StandardCharsets.UTF_8).length, Person.class));
        assertEquals(person, XmlMappers.fromXml(xml, Person.class, XmlMappers.createDeserializationConfig()));
        assertEquals(person, XmlMappers.fromXml(xml, Person.class, (DeserializationConfig) null));
    }

    @Test
    public void testFromXml_ByteArrayOffset() {
        final Person person = new Person("Frank", 45);
        final String xml = XmlMappers.toXml(person);
        final byte[] buffered = ("xxx" + xml + "yyy").getBytes(StandardCharsets.UTF_8);
        assertEquals(person, XmlMappers.fromXml(buffered, 3, xml.getBytes(StandardCharsets.UTF_8).length, Person.class));
    }

    @Test
    public void testFromXml_DeserializationFeatures() {
        final String xml = "<Person><name>John</name><unknownField>value</unknownField><age>30</age></Person>";
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(xml, Person.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    public void testFromXml_TypeReference() {
        final List<String> list = CommonUtil.toList("a", "b", "c");
        final String xml = XmlMappers.toXml(list);
        final TypeReference<List<String>> type = new TypeReference<>() {
        };
        assertEquals(3, XmlMappers.fromXml(xml, type).size());
        assertEquals(3, XmlMappers.fromXml(xml.getBytes(StandardCharsets.UTF_8), type).size());
        assertEquals(3, XmlMappers.fromXml(xml.getBytes(StandardCharsets.UTF_8), 0, xml.getBytes(StandardCharsets.UTF_8).length, type).size());
        assertEquals(3, XmlMappers.fromXml(xml, type, XmlMappers.createDeserializationConfig()).size());
        assertEquals(3, XmlMappers.fromXml(xml, type, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).size());
    }

    @Test
    public void testFromXml_Sources() throws IOException {
        final Person person = new Person("Henry", 38);
        final File file = xmlFile("fromXml.xml", person);
        assertEquals(person, XmlMappers.fromXml(file, Person.class));
        assertEquals(person, XmlMappers.fromXml(file, Person.class, XmlMappers.createDeserializationConfig()));

        final String xml = XmlMappers.toXml(person);
        final boolean[] closed = { false };
        final ByteArrayInputStream autoClose = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        assertEquals(person, XmlMappers.fromXml(autoClose, Person.class));
        assertTrue(closed[0]);

        assertEquals(person,
                XmlMappers.fromXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), Person.class, XmlMappers.createDeserializationConfig()));
        assertEquals(person, XmlMappers.fromXml(new StringReader(xml), Person.class));
        assertEquals(person, XmlMappers.fromXml(new StringReader(xml), Person.class, XmlMappers.createDeserializationConfig()));

        final URL url = file.toURI().toURL();
        assertEquals(person, XmlMappers.fromXml(url, Person.class));
        assertEquals(person, XmlMappers.fromXml(url, Person.class, XmlMappers.createDeserializationConfig()));
    }

    @Test
    public void testFromXml_Sources_TypeReference() throws IOException {
        final List<String> list = CommonUtil.toList("alpha", "beta", "gamma");
        final TypeReference<List<String>> type = new TypeReference<>() {
        };
        final File file = xmlFile("typeRef.xml", list);
        assertEquals(3, XmlMappers.fromXml(file, type).size());
        assertEquals(3, XmlMappers.fromXml(file, type, XmlMappers.createDeserializationConfig()).size());

        final String xml = XmlMappers.toXml(list);
        assertEquals(3, XmlMappers.fromXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), type).size());
        assertEquals(3,
                XmlMappers.fromXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), type, XmlMappers.createDeserializationConfig()).size());
        assertEquals(3, XmlMappers.fromXml(new StringReader(xml), type).size());
        assertEquals(3, XmlMappers.fromXml(new StringReader(xml), type, XmlMappers.createDeserializationConfig()).size());
        assertEquals(3, XmlMappers.fromXml(file.toURI().toURL(), type).size());
        assertEquals(3, XmlMappers.fromXml(file.toURI().toURL(), type, XmlMappers.createDeserializationConfig()).size());
    }

    @Test
    public void testFromXml_DataInput_Unsupported() throws IOException {
        final String xml = XmlMappers.toXml(new Person("Victor", 44));
        final DataInput classInput = new DataInputStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(classInput, Person.class));

        final DataInput classConfig = new DataInputStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(classConfig, Person.class, XmlMappers.createDeserializationConfig()));

        final TypeReference<List<String>> type = new TypeReference<>() {
        };
        final String listXml = XmlMappers.toXml(CommonUtil.toList("monday", "tuesday"));
        final DataInput typeInput = new DataInputStream(new ByteArrayInputStream(listXml.getBytes(StandardCharsets.UTF_8)));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(typeInput, type));

        final DataInput typeConfig = new DataInputStream(new ByteArrayInputStream(listXml.getBytes(StandardCharsets.UTF_8)));
        assertThrows(UnsupportedOperationException.class, () -> XmlMappers.fromXml(typeConfig, type, XmlMappers.createDeserializationConfig()));
    }

    @Test
    public void testCreateSerializationConfig() {
        final SerializationConfig config = XmlMappers.createSerializationConfig();
        assertNotSame(config, XmlMappers.createSerializationConfig());
        assertSame(config, config.with(SerializationFeature.FAIL_ON_EMPTY_BEANS));
        assertNotSame(config, config.with(SerializationFeature.INDENT_OUTPUT));
    }

    @Test
    public void testCreateDeserializationConfig() {
        final DeserializationConfig config = XmlMappers.createDeserializationConfig();
        assertNotSame(config, XmlMappers.createDeserializationConfig());
        assertSame(config, config.with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        assertNotSame(config, config.with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY));
    }

    @Test
    public void testWrap() {
        assertNotNull(XmlMappers.wrap(new XmlMapper()));
    }

    @Test
    public void testOne_ToXml() throws IOException {
        final XmlMappers.One wrapper = one();
        final Person person = new Person("John", 30);
        final String xml = wrapper.toXml(person);
        assertTrue(xml.contains("John"));

        final String pretty = wrapper.toXml(person, true);
        final String compact = wrapper.toXml(person, false);
        assertTrue(pretty.contains("\n"));
        assertFalse(compact.contains("\n"));

        final File file = new File(tempDir, "one.xml");
        wrapper.toXml(person, file);
        assertTrue(file.length() > 0);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wrapper.toXml(person, baos);
        assertTrue(baos.toString().contains("John"));

        final StringWriter writer = new StringWriter();
        wrapper.toXml(person, writer);
        assertTrue(writer.toString().contains("John"));

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        wrapper.toXml(person, (DataOutput) new DataOutputStream(data));
        assertTrue(data.toString().contains("John"));
    }

    @Test
    public void testOne_FromXml() throws IOException {
        final XmlMappers.One wrapper = one();
        final Person person = new Person("Eve", 27);
        final String xml = wrapper.toXml(person);
        assertEquals(person, wrapper.fromXml(xml, Person.class));
        assertEquals(person, wrapper.fromXml(xml.getBytes(StandardCharsets.UTF_8), Person.class));
        assertEquals(person, wrapper.fromXml(xml.getBytes(StandardCharsets.UTF_8), 0, xml.getBytes(StandardCharsets.UTF_8).length, Person.class));

        final byte[] buffered = ("xxx" + xml + "yyy").getBytes(StandardCharsets.UTF_8);
        assertEquals(person, wrapper.fromXml(buffered, 3, xml.getBytes(StandardCharsets.UTF_8).length, Person.class));

        final File file = xmlFile("one-from.xml", person);
        assertEquals(person, wrapper.fromXml(file, Person.class));
        assertEquals(person, wrapper.fromXml(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), Person.class));
        assertEquals(person, wrapper.fromXml(new StringReader(xml), Person.class));
        assertEquals(person, wrapper.fromXml(file.toURI().toURL(), Person.class));

        final List<String> list = CommonUtil.toList("a", "b", "c");
        final TypeReference<List<String>> type = new TypeReference<>() {
        };
        final String listXml = wrapper.toXml(list);
        assertEquals(3, wrapper.fromXml(listXml, type).size());
        assertEquals(3, wrapper.fromXml(listXml.getBytes(StandardCharsets.UTF_8), type).size());
        assertEquals(3, wrapper.fromXml(listXml.getBytes(StandardCharsets.UTF_8), 0, listXml.getBytes(StandardCharsets.UTF_8).length, type).size());
        final File listFile = xmlFile("one-typeRef.xml", list);
        assertEquals(3, wrapper.fromXml(listFile, type).size());
        assertEquals(3, wrapper.fromXml(new ByteArrayInputStream(listXml.getBytes(StandardCharsets.UTF_8)), type).size());
        assertEquals(3, wrapper.fromXml(new StringReader(listXml), type).size());
        assertEquals(3, wrapper.fromXml(listFile.toURI().toURL(), type).size());

        final DataInput classInput = new DataInputStream(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(classInput, Person.class));
        final DataInput typeInput = new DataInputStream(new ByteArrayInputStream(listXml.getBytes(StandardCharsets.UTF_8)));
        assertThrows(UnsupportedOperationException.class, () -> wrapper.fromXml(typeInput, type));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRecycle_DoesNotPoolSharedDefaultMapperOnNullConfig() throws Exception {
        final java.lang.reflect.Field serializationPoolField = XmlMappers.class.getDeclaredField("serializationMapperPool");
        final java.lang.reflect.Field deserializationPoolField = XmlMappers.class.getDeclaredField("deserializationMapperPool");
        serializationPoolField.setAccessible(true);
        deserializationPoolField.setAccessible(true);
        final java.lang.reflect.Field defField = XmlMappers.class.getDeclaredField("defaultXmlMapper");
        defField.setAccessible(true);

        final Map<SerializationConfig, XmlMapper> serializationPool = (Map<SerializationConfig, XmlMapper>) serializationPoolField.get(null);
        final Map<DeserializationConfig, XmlMapper> deserializationPool = (Map<DeserializationConfig, XmlMapper>) deserializationPoolField.get(null);
        final XmlMapper sharedDefault = (XmlMapper) defField.get(null);

        synchronized (serializationPool) {
            serializationPool.clear();
        }
        synchronized (deserializationPool) {
            deserializationPool.clear();
        }

        XmlMappers.toXml(new Person("Pooling", 1), (SerializationConfig) null);

        synchronized (serializationPool) {
            assertTrue(serializationPool.isEmpty());
            assertFalse(serializationPool.containsValue(sharedDefault));
        }
        synchronized (deserializationPool) {
            assertTrue(deserializationPool.isEmpty());
            assertFalse(deserializationPool.containsValue(sharedDefault));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLibraryOwnedXmlMappersUseHardenedInputFactories() throws Exception {
        final java.lang.reflect.Field defaultField = XmlMappers.class.getDeclaredField("defaultXmlMapper");
        final java.lang.reflect.Field poolField = XmlMappers.class.getDeclaredField("deserializationMapperPool");
        defaultField.setAccessible(true);
        poolField.setAccessible(true);

        final XmlMapper sharedDefault = (XmlMapper) defaultField.get(null);
        assertSecureXmlInputFactory(sharedDefault);

        assertThrows(NoSuchFieldException.class, () -> XmlMappers.class.getDeclaredField("defaultXmlMapperForPretty"));
        final java.lang.reflect.Field prettyWriterField = XmlMappers.class.getDeclaredField("defaultXmlWriterForPretty");
        prettyWriterField.setAccessible(true);
        assertNotNull(prettyWriterField.get(null));

        final Map<DeserializationConfig, XmlMapper> pool = (Map<DeserializationConfig, XmlMapper>) poolField.get(null);
        synchronized (pool) {
            pool.clear();
        }

        final DeserializationConfig config = XmlMappers.createDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        XmlMappers.fromXml("<Person><name>safe</name><age>1</age></Person>", Person.class, config);

        synchronized (pool) {
            assertEquals(1, pool.size());
            final XmlMapper pooled = pool.values().iterator().next();
            assertSecureXmlInputFactory(pooled);
            assertSame(sharedDefault.getFactory().getXMLInputFactory(), pooled.getFactory().getXMLInputFactory());
        }
    }

    @Test
    public void testDisabledDtdProcessingDoesNotRejectPlainDoctype() {
        final String xml = "<!DOCTYPE Person><Person><name>safe</name><age>1</age></Person>";
        final Person person = new Person("safe", 1);

        assertEquals(person, XmlMappers.fromXml(xml, Person.class));
        assertEquals(person, XmlMappers.fromXml(xml, Person.class, XmlMappers.createDeserializationConfig()));
        assertEquals(person, XmlMappers.fromXml(xml, Person.class, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
    }

    @Test
    public void testLibraryOwnedXmlMappersRejectExternalEntityPayloads() {
        final String hostileXml = "<!DOCTYPE Person [<!ENTITY xxe SYSTEM \"file:///definitely-not-readable-abacus-xxe\">]>"
                + "<Person><name>&xxe;</name><age>1</age></Person>";

        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(hostileXml, Person.class));

        final DeserializationConfig config = XmlMappers.createDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        assertThrows(RuntimeException.class, () -> XmlMappers.fromXml(hostileXml, Person.class, config));
    }

    @Test
    public void testWrapDoesNotMutateCallerOwnedXmlInputFactory() {
        final XMLInputFactory callerFactory = XMLInputFactory.newFactory();
        callerFactory.setProperty(XMLInputFactory.SUPPORT_DTD, true);
        final XmlMapper callerMapper = new XmlMapper(callerFactory);

        XmlMappers.wrap(callerMapper);

        assertSame(callerFactory, callerMapper.getFactory().getXMLInputFactory());
        assertEquals(Boolean.TRUE, callerFactory.getProperty(XMLInputFactory.SUPPORT_DTD));
    }

    @Test
    public void testRecycleIsPrivateSoCallerOwnedMappersCannotPoisonPool() throws Exception {
        assertTrue(Modifier.isPrivate(XmlMappers.class.getDeclaredMethod("recycle", XmlMapper.class).getModifiers()));
    }

    private static void assertSecureXmlInputFactory(final XmlMapper mapper) throws XMLStreamException {
        final XMLInputFactory factory = mapper.getFactory().getXMLInputFactory();
        assertEquals(Boolean.FALSE, factory.getProperty(XMLInputFactory.SUPPORT_DTD));
        assertEquals(Boolean.FALSE, factory.getProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES));
        assertThrows(XMLStreamException.class, () -> factory.getXMLResolver().resolveEntity("public", "system", "base", "namespace"));
    }
}
