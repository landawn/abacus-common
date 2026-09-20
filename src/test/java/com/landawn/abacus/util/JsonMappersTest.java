package com.landawn.abacus.util;

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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.landawn.abacus.TestBase;

public class JsonMappersTest extends TestBase {

    public static class Person {
        public String name;
        public Integer age;

        public Person() {
        }

        public Person(final String name, final Integer age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Person person = (Person) o;
            return Objects.equals(name, person.name) && Objects.equals(age, person.age);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }
    }

    public static class HiddenFieldBean {
        private String value;

        public HiddenFieldBean() {
        }

        HiddenFieldBean(final String value) {
            this.value = value;
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

    @TempDir
    File tempDir;

    private static DataInput dataInput(final byte[] bytes) {
        return new DataInputStream(new ByteArrayInputStream(bytes));
    }

    private static JsonMappers.One one() {
        return JsonMappers.wrap(new ObjectMapper());
    }

    private File tempFile(final String name, final String content) throws IOException {
        final File file = new File(tempDir, name);
        Files.writeString(file.toPath(), content);
        return file;
    }

    @Test
    public void testSerializationConfigurationDoesNotLeakThroughMapperCache() {
        final ObjectMapper fieldMapper = new ObjectMapper();
        fieldMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        final SerializationConfig fieldConfig = fieldMapper.getSerializationConfig();

        Assertions.assertEquals("{\"value\":\"secret\"}", JsonMappers.toJson(new HiddenFieldBean("secret"), fieldConfig));

        final SerializationConfig defaultConfig = JsonMappers.createSerializationConfig();
        Assertions.assertThrows(RuntimeException.class, () -> JsonMappers.toJson(new HiddenFieldBean("secret"), defaultConfig));
    }

    @Test
    public void testDeserializationConfigurationDoesNotLeakThroughMapperCache() {
        final ObjectMapper fieldMapper = new ObjectMapper();
        fieldMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        final DeserializationConfig fieldConfig = fieldMapper.getDeserializationConfig();

        final HiddenFieldBean bean = JsonMappers.fromJson("{\"value\":\"secret\"}", HiddenFieldBean.class, fieldConfig);
        Assertions.assertEquals("secret", bean.value);

        final DeserializationConfig defaultConfig = JsonMappers.createDeserializationConfig();
        Assertions.assertThrows(RuntimeException.class, () -> JsonMappers.fromJson("{\"value\":\"secret\"}", HiddenFieldBean.class, defaultConfig));
    }

    @Test
    public void testFromJsonDataInputWithTypeReferenceReportsNullTargetTypeLikeItsSiblings() {
        final byte[] bytes = "[]".getBytes(StandardCharsets.UTF_8);
        final DeserializationConfig config = JsonMappers.createDeserializationConfig();
        final JsonMappers.One wrapper = one();

        // These three were the only TypeReference overloads left without an explicit guard. Jackson already threw
        // an IllegalArgumentException of its own here, so the exception type proves nothing - the message is what
        // pins them to the same guard the other 20 use (Jackson's own reads: argument "typeRef" is null).
        Assertions.assertEquals("'targetType' cannot be null",
                Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(dataInput(bytes), (TypeReference<?>) null)).getMessage());
        Assertions.assertEquals("'targetType' cannot be null",
                Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(dataInput(bytes), (TypeReference<?>) null, config))
                        .getMessage());
        Assertions.assertEquals("'targetType' cannot be null",
                Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(dataInput(bytes), (TypeReference<?>) null)).getMessage());
    }

    @Test
    public void testCreateSerializationConfigDoesNotShareMutableStateWithDefaultMapper() {
        final SerializationConfig mine = JsonMappers.createSerializationConfig();
        final SerializationConfig other = JsonMappers.createSerializationConfig();
        final JsonInclude.Value nonNull = JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL);

        try {
            // withPropertyInclusion(..) is the one Jackson config method that writes through the config's
            // ConfigOverrides in place and returns the receiver instead of a copy.
            Assertions.assertSame(mine, mine.withPropertyInclusion(nonNull));

            // none of the entry points backed by this class's own mapper may see that mutation
            Assertions.assertEquals("{\"value\":\"x\",\"missing\":null}", JsonMappers.toJson(new InclusionBeanPlain()));
            Assertions.assertEquals("{\"value\":\"x\",\"missing\":null}", JsonMappers.toJson(new InclusionBeanPretty(), true).replaceAll("\\s", ""));
            Assertions.assertEquals("{\"value\":\"x\",\"missing\":null}", JsonMappers.toJson(new InclusionBeanNullConfig(), (SerializationConfig) null));
            Assertions.assertEquals("{\"value\":\"x\",\"missing\":null}",
                    JsonMappers.toJson(new InclusionBeanFeature(), SerializationFeature.WRITE_ENUMS_USING_TO_STRING));

            // nor may any other config handed out by the factories, before or after the mutation
            Assertions.assertEquals(JsonInclude.Include.NON_NULL, mine.getDefaultPropertyInclusion().getValueInclusion());
            Assertions.assertEquals(JsonInclude.Include.USE_DEFAULTS, other.getDefaultPropertyInclusion().getValueInclusion());
            Assertions.assertEquals(JsonInclude.Include.USE_DEFAULTS,
                    JsonMappers.createSerializationConfig().getDefaultPropertyInclusion().getValueInclusion());
            Assertions.assertEquals(JsonInclude.Include.USE_DEFAULTS,
                    JsonMappers.createDeserializationConfig().getDefaultPropertyInclusion().getValueInclusion());

            // the caller that asked for NON_NULL still gets exactly what it asked for
            Assertions.assertEquals("{\"value\":\"x\"}", JsonMappers.toJson(new InclusionBeanOwnConfig(), mine));
        } finally {
            // an unfixed build shares this object with the default mapper: put it back rather than leaving
            // the rest of the suite to run against a mutated default inclusion
            mine.withPropertyInclusion(JsonInclude.Value.empty());
        }
    }

    @Test
    public void testCreateConfigHandsOutJacksonsProcessWideObjectsByReference() {
        // The flip side of the isolation above, and the reason createSerializationConfig()'s javadoc must not
        // promise that a returned config shares NO mutable state: its own ConfigOverrides is copied, but
        // everything it hands out is still Jackson's JVM-wide default. Identity only - mutating any of these
        // really does change toJson(..) for the whole process, which is exactly why no test may do it.
        final SerializationConfig ser = JsonMappers.createSerializationConfig();
        final DeserializationConfig deser = JsonMappers.createDeserializationConfig();
        final ObjectMapper untouched = new ObjectMapper();

        Assertions.assertSame(ser.getDefaultPrettyPrinter(), JsonMappers.createSerializationConfig().getDefaultPrettyPrinter());
        Assertions.assertSame(ser.getDefaultPrettyPrinter(), untouched.getSerializationConfig().getDefaultPrettyPrinter());

        Assertions.assertSame(ser.getDateFormat(), deser.getDateFormat());
        Assertions.assertSame(ser.getDateFormat(), untouched.getSerializationConfig().getDateFormat());

        Assertions.assertSame(ser.getAnnotationIntrospector(), deser.getAnnotationIntrospector());
        Assertions.assertSame(ser.getAnnotationIntrospector(), untouched.getSerializationConfig().getAnnotationIntrospector());
    }

    @Test
    public void testToJson() {
        final Person person = new Person("John", 30);
        final String json = JsonMappers.toJson(person);
        Assertions.assertTrue(json.contains("\"name\":\"John\""));
        Assertions.assertTrue(json.contains("\"age\":30"));
        Assertions.assertEquals("null", JsonMappers.toJson(null));
        Assertions.assertEquals("42", JsonMappers.toJson(42));
        Assertions.assertEquals("test", JsonMappers.fromJson(JsonMappers.toJson("test"), String.class));
        Assertions.assertEquals(person, JsonMappers.fromJson(json, Person.class));

        final List<Person> empty = new ArrayList<>();
        Assertions.assertEquals(0, JsonMappers.fromJson(JsonMappers.toJson(empty), new TypeReference<List<Person>>() {
        }).size());

        final List<Person> people = new ArrayList<>();
        people.add(new Person("List1", 25));
        people.add(new Person("List2", 26));
        Assertions.assertEquals(2, JsonMappers.fromJson(JsonMappers.toJson(people), new TypeReference<List<Person>>() {
        }).size());

        final Map<String, Person> map = new HashMap<>();
        map.put("person1", new Person("Map1", 20));
        map.put("person2", new Person("Map2", 21));
        final Map<String, Person> deserialized = JsonMappers.fromJson(JsonMappers.toJson(map), new TypeReference<Map<String, Person>>() {
        });
        Assertions.assertEquals(2, deserialized.size());
        Assertions.assertEquals("Map1", deserialized.get("person1").name);
    }

    @Test
    public void testToJson_PrettyFormat() {
        final Person person = new Person("Alice", 25);
        final String compact = JsonMappers.toJson(person, false);
        final String pretty = JsonMappers.toJson(person, true);

        Assertions.assertFalse(compact.contains("\n"));
        Assertions.assertTrue(pretty.contains("\n"));
        Assertions.assertTrue(pretty.contains("Alice"));
    }

    @Test
    public void testToJson_SerializationFeatures() {
        final Person person = new Person("John", null);
        final String json = JsonMappers.toJson(person, SerializationFeature.WRITE_NULL_MAP_VALUES, SerializationFeature.INDENT_OUTPUT);
        Assertions.assertTrue(json.contains("John"));
        Assertions.assertTrue(json.contains("\n") || json.contains("  "));
    }

    @Test
    public void testToJson_SerializationConfig() {
        final Person person = new Person("Bob", 40);
        final SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
        final String json = JsonMappers.toJson(person, config);
        Assertions.assertTrue(json.contains("Bob"));
        Assertions.assertTrue(json.contains("\n"));
    }

    @Test
    public void testToJson_File() throws IOException {
        final Person person = new Person("Charlie", 35);
        final File outputFile = new File(tempDir, "person.json");
        JsonMappers.toJson(person, outputFile);
        Assertions.assertTrue(Files.readString(outputFile.toPath()).contains("Charlie"));

        final File prettyFile = new File(tempDir, "person-pretty.json");
        JsonMappers.toJson(person, prettyFile, JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT));
        Assertions.assertTrue(Files.readString(prettyFile.toPath()).contains("Charlie"));
    }

    @Test
    public void testToJson_OutputStream() throws IOException {
        final boolean[] closed = { false };
        final ByteArrayOutputStream baos = new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };

        JsonMappers.toJson(new Person("Eve", 32), baos);
        Assertions.assertTrue(closed[0]);
        Assertions.assertTrue(baos.toString(StandardCharsets.UTF_8.name()).contains("Eve"));

        final ByteArrayOutputStream configured = new ByteArrayOutputStream();
        JsonMappers.toJson(new Person("Frank", 45), configured, JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT));
        Assertions.assertTrue(configured.toString(StandardCharsets.UTF_8.name()).contains("Frank"));
    }

    @Test
    public void testToJson_Writer() throws IOException {
        final StringWriter writer = new StringWriter();
        JsonMappers.toJson(new Person("Grace", 29), writer);
        Assertions.assertTrue(writer.toString().contains("Grace"));

        final StringWriter configured = new StringWriter();
        JsonMappers.toJson(new Person("Henry", 50), configured, JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT));
        Assertions.assertTrue(configured.toString().contains("Henry"));
    }

    @Test
    public void testToJson_DataOutput() throws IOException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonMappers.toJson(new Person("Ivy", 27), (DataOutput) new DataOutputStream(baos));
        Assertions.assertTrue(baos.toString(StandardCharsets.UTF_8.name()).contains("Ivy"));

        final ByteArrayOutputStream configured = new ByteArrayOutputStream();
        JsonMappers.toJson(new Person("Jack", 33), (DataOutput) new DataOutputStream(configured), JsonMappers.createSerializationConfig());
        Assertions.assertTrue(configured.toString(StandardCharsets.UTF_8.name()).contains("Jack"));

        final ByteArrayOutputStream binary = new ByteArrayOutputStream();
        final DataOutputStream output = new DataOutputStream(binary);
        output.writeInt(42);
        JsonMappers.toJson(new Person("Ivy", 27), (DataOutput) output);
        final DataInputStream input = new DataInputStream(new ByteArrayInputStream(binary.toByteArray()));
        Assertions.assertEquals(42, input.readInt());
        final Person decoded = JsonMappers.fromJson((DataInput) input, Person.class);
        Assertions.assertEquals("Ivy", decoded.name);
        Assertions.assertEquals(27, decoded.age);
    }

    @Test
    public void testFromJson() {
        final Person person = JsonMappers.fromJson("{\"name\":\"Mary\",\"age\":24}", Person.class);
        Assertions.assertEquals("Mary", person.name);
        Assertions.assertEquals(24, person.age);
        Assertions.assertNull(JsonMappers.fromJson("null", Person.class));
    }

    @Test
    public void testFromJson_ByteArray() {
        final byte[] json = "{\"name\":\"Kate\",\"age\":26}".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals("Kate", JsonMappers.fromJson(json, Person.class).name);

        final byte[] buffered = "XXXX{\"name\":\"Leo\",\"age\":31}YYYY".getBytes(StandardCharsets.UTF_8);
        final Person person = JsonMappers.fromJson(buffered, 4, "{\"name\":\"Leo\",\"age\":31}".length(), Person.class);
        Assertions.assertEquals("Leo", person.name);
        Assertions.assertEquals(31, person.age);
    }

    @Test
    public void testFromJson_DeserializationFeatures() {
        final String json = "{\"name\":\"Nancy\",\"age\":22,\"unknown\":\"field\"}";
        final Person ignored = JsonMappers.fromJson(json, Person.class,
                JsonMappers.createDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));
        Assertions.assertEquals("Nancy", ignored.name);

        final Person features = JsonMappers.fromJson("{\"name\":\"John\",\"age\":30}", Person.class, DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
        Assertions.assertEquals("John", features.name);
    }

    @Test
    public void testFromJson_DeserializationConfig() {
        final DeserializationConfig config = JsonMappers.createDeserializationConfig();
        final Person person = JsonMappers.fromJson("{\"name\":\"Oscar\",\"age\":38}", Person.class, config);
        Assertions.assertEquals("Oscar", person.name);
        Assertions.assertEquals(38, person.age);
    }

    @Test
    public void testFromJson_TypeReference() {
        final String json = "[{\"name\":\"Carol\",\"age\":31},{\"name\":\"David\",\"age\":33}]";
        final List<Person> people = JsonMappers.fromJson(json, new TypeReference<List<Person>>() {
        });
        Assertions.assertEquals(2, people.size());
        Assertions.assertEquals("Carol", people.get(0).name);

        final List<Person> withFeatures = JsonMappers.fromJson("[{\"name\":\"Emma\",\"age\":25}]", new TypeReference<List<Person>>() {
        }, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Assertions.assertEquals(1, withFeatures.size());

        final List<Person> withConfig = JsonMappers.fromJson("[{\"name\":\"Fiona\",\"age\":37}]", new TypeReference<List<Person>>() {
        }, JsonMappers.createDeserializationConfig());
        Assertions.assertEquals("Fiona", withConfig.get(0).name);

        final byte[] bytes = "[{\"name\":\"Zoe\",\"age\":30}]".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals("Zoe", JsonMappers.fromJson(bytes, new TypeReference<List<Person>>() {
        }).get(0).name);

        final byte[] buffered = "XXX[{\"name\":\"Ben\",\"age\":28}]YYY".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals("Ben", JsonMappers.fromJson(buffered, 3, "[{\"name\":\"Ben\",\"age\":28}]".length(), new TypeReference<List<Person>>() {
        }).get(0).name);

        final Map<String, String> map = JsonMappers.fromJson("{\"key1\":\"value1\",\"key2\":\"value2\"}", new TypeReference<Map<String, String>>() {
        });
        Assertions.assertEquals("value1", map.get("key1"));
    }

    @Test
    public void testFromJson_Sources() throws IOException {
        final String json = "{\"name\":\"Paul\",\"age\":42}";
        final File file = tempFile("test-person.json", json);
        Assertions.assertEquals("Paul", JsonMappers.fromJson(file, Person.class).name);
        Assertions.assertEquals("Paul", JsonMappers.fromJson(file, Person.class, JsonMappers.createDeserializationConfig()).name);

        final boolean[] closed = { false };
        final ByteArrayInputStream autoClose = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() throws IOException {
                closed[0] = true;
                super.close();
            }
        };
        Assertions.assertEquals("Paul", JsonMappers.fromJson(autoClose, Person.class).name);
        Assertions.assertTrue(closed[0]);

        Assertions.assertEquals("Paul", JsonMappers.fromJson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), Person.class,
                JsonMappers.createDeserializationConfig()).name);
        Assertions.assertEquals("Paul", JsonMappers.fromJson(new StringReader(json), Person.class).name);
        Assertions.assertEquals("Paul", JsonMappers.fromJson(new StringReader(json), Person.class, JsonMappers.createDeserializationConfig()).name);

        final URL url = file.toURI().toURL();
        Assertions.assertEquals("Paul", JsonMappers.fromJson(url, Person.class).name);
        Assertions.assertEquals("Paul", JsonMappers.fromJson(url, Person.class, JsonMappers.createDeserializationConfig()).name);

        Assertions.assertEquals("Paul",
                JsonMappers.fromJson((DataInput) new DataInputStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))), Person.class).name);
        Assertions.assertEquals("Paul", JsonMappers.fromJson((DataInput) new DataInputStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))),
                Person.class, JsonMappers.createDeserializationConfig()).name);
    }

    @Test
    public void testFromJson_Sources_TypeReference() throws IOException {
        final String json = "[{\"name\":\"Rachel\",\"age\":29}]";
        final TypeReference<List<Person>> type = new TypeReference<>() {
        };
        final File file = tempFile("test-list.json", json);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(file, type).get(0).name);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(file, type, JsonMappers.createDeserializationConfig()).get(0).name);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), type).get(0).name);
        Assertions.assertEquals("Rachel",
                JsonMappers.fromJson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), type, JsonMappers.createDeserializationConfig())
                        .get(0).name);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(new StringReader(json), type).get(0).name);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(new StringReader(json), type, JsonMappers.createDeserializationConfig()).get(0).name);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(file.toURI().toURL(), type).get(0).name);
        Assertions.assertEquals("Rachel", JsonMappers.fromJson(file.toURI().toURL(), type, JsonMappers.createDeserializationConfig()).get(0).name);
        Assertions.assertEquals("Rachel",
                JsonMappers.fromJson((DataInput) new DataInputStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))), type).get(0).name);
        Assertions.assertEquals("Rachel",
                JsonMappers
                        .fromJson((DataInput) new DataInputStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))), type,
                                JsonMappers.createDeserializationConfig())
                        .get(0).name);
    }

    @Test
    public void testFromJsonWithTypeReferenceRejectsNullTargetType() throws IOException {
        final String json = "[]";
        final byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        final File file = tempFile("null-target-type.json", json);
        final DeserializationConfig config = JsonMappers.createDeserializationConfig();
        final JsonMappers.One wrapper = one();

        // a null targetType is an argument error on every TypeReference overload, with the same message on all of
        // them - not a raw NullPointerException out of Jackson, and not Jackson's own 'argument "typeRef" is null'
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(json, (TypeReference<?>) null));
        Assertions.assertEquals("'targetType' cannot be null", e.getMessage());

        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(bytes, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(bytes, 0, bytes.length, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(file, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(new ByteArrayInputStream(bytes), (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(new StringReader(json), (TypeReference<?>) null));

        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(json, (TypeReference<?>) null, config));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(file, (TypeReference<?>) null, config));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(new ByteArrayInputStream(bytes), (TypeReference<?>) null, config));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(new StringReader(json), (TypeReference<?>) null, config));

        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(json, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(bytes, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(bytes, 0, bytes.length, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(file, (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(new ByteArrayInputStream(bytes), (TypeReference<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> wrapper.fromJson(new StringReader(json), (TypeReference<?>) null));

        // unchanged shapes, kept here so the two families stay pinned together
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(json, (Class<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(file.toURI().toURL(), (TypeReference<?>) null));
    }

    @Test
    public void testCreateSerializationConfig() {
        Assertions.assertNotNull(JsonMappers.createSerializationConfig());
    }

    @Test
    public void testCreateDeserializationConfig() {
        Assertions.assertNotNull(JsonMappers.createDeserializationConfig());
    }

    @Test
    public void testWrap() {
        Assertions.assertNotNull(JsonMappers.wrap(new ObjectMapper()));
    }

    @Test
    public void testOne_ToJson() throws IOException {
        final JsonMappers.One wrapper = one();
        final Person person = new Person("Alice", 25);
        final String json = wrapper.toJson(person);
        Assertions.assertTrue(json.contains("Alice"));

        final String pretty = wrapper.toJson(person, true);
        final String compact = wrapper.toJson(person, false);
        Assertions.assertTrue(pretty.contains("\n"));
        Assertions.assertFalse(compact.contains("\n"));

        final File file = new File(tempDir, "one-person.json");
        wrapper.toJson(person, file);
        Assertions.assertTrue(Files.readString(file.toPath()).contains("Alice"));

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        wrapper.toJson(person, baos);
        Assertions.assertTrue(baos.toString(StandardCharsets.UTF_8.name()).contains("Alice"));

        final StringWriter writer = new StringWriter();
        wrapper.toJson(person, writer);
        Assertions.assertTrue(writer.toString().contains("Alice"));

        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        wrapper.toJson(person, (DataOutput) new DataOutputStream(data));
        Assertions.assertTrue(data.toString(StandardCharsets.UTF_8.name()).contains("Alice"));
    }

    @Test
    public void testOne_FromJson() throws IOException {
        final JsonMappers.One wrapper = one();
        final String json = "{\"name\":\"Grace\",\"age\":29}";
        Assertions.assertEquals("Grace", wrapper.fromJson(json, Person.class).name);
        Assertions.assertEquals("Grace", wrapper.fromJson(json.getBytes(StandardCharsets.UTF_8), Person.class).name);

        final byte[] buffered = "XX{\"name\":\"Henry\",\"age\":50}".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals("Henry", wrapper.fromJson(buffered, 2, "{\"name\":\"Henry\",\"age\":50}".length(), Person.class).name);

        final TypeReference<List<Person>> type = new TypeReference<>() {
        };
        Assertions.assertEquals("Oscar", wrapper.fromJson("[{\"name\":\"Oscar\",\"age\":38}]", type).get(0).name);
        Assertions.assertEquals("Oscar", wrapper.fromJson("[{\"name\":\"Oscar\",\"age\":38}]".getBytes(StandardCharsets.UTF_8), type).get(0).name);
        final byte[] typeBuffered = "YY[{\"name\":\"Paul\",\"age\":42}]".getBytes(StandardCharsets.UTF_8);
        Assertions.assertEquals("Paul", wrapper.fromJson(typeBuffered, 2, "[{\"name\":\"Paul\",\"age\":42}]".length(), type).get(0).name);

        final File file = tempFile("one-test.json", json);
        Assertions.assertEquals("Grace", wrapper.fromJson(file, Person.class).name);
        Assertions.assertEquals("Grace", wrapper.fromJson(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)), Person.class).name);
        Assertions.assertEquals("Grace", wrapper.fromJson(new StringReader(json), Person.class).name);
        Assertions.assertEquals("Grace", wrapper.fromJson(file.toURI().toURL(), Person.class).name);
        Assertions.assertEquals("Grace",
                wrapper.fromJson((DataInput) new DataInputStream(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8))), Person.class).name);

        final String listJson = "[{\"name\":\"Rachel\",\"age\":29}]";
        final File listFile = tempFile("one-list.json", listJson);
        Assertions.assertEquals("Rachel", wrapper.fromJson(listFile, type).get(0).name);
        Assertions.assertEquals("Rachel", wrapper.fromJson(new ByteArrayInputStream(listJson.getBytes(StandardCharsets.UTF_8)), type).get(0).name);
        Assertions.assertEquals("Rachel", wrapper.fromJson(new StringReader(listJson), type).get(0).name);
        Assertions.assertEquals("Rachel", wrapper.fromJson(listFile.toURI().toURL(), type).get(0).name);
        Assertions.assertEquals("Rachel",
                wrapper.fromJson((DataInput) new DataInputStream(new ByteArrayInputStream(listJson.getBytes(StandardCharsets.UTF_8))), type).get(0).name);
    }

    @Test
    public void testByteArraySegmentsAreValidatedConsistently() {
        final byte[] json = "xx{\"name\":\"Ada\",\"age\":37}yy".getBytes(StandardCharsets.UTF_8);
        final int offset = 2;
        final int len = json.length - 4;
        final TypeReference<Person> type = new TypeReference<>() {
        };
        final JsonMappers.One wrapper = one();

        Assertions.assertEquals("Ada", JsonMappers.fromJson(json, offset, len, Person.class).name);
        Assertions.assertEquals("Ada", JsonMappers.fromJson(json, offset, len, type).name);
        Assertions.assertEquals("Ada", wrapper.fromJson(json, offset, len, Person.class).name);
        Assertions.assertEquals("Ada", wrapper.fromJson(json, offset, len, type).name);

        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson((byte[]) null, 0, 0, Person.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson(json, 0, -1, Person.class));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> JsonMappers.fromJson(json, -1, 1, type));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> wrapper.fromJson(json, json.length, 1, Person.class));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> wrapper.fromJson(json, 1, Integer.MAX_VALUE, type));
    }

    @Test
    public void testFeatureAndMapperArgumentsAreValidated() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> JsonMappers.toJson(new Person(), (SerializationFeature) null, new SerializationFeature[0]));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> JsonMappers.toJson(new Person(), SerializationFeature.INDENT_OUTPUT, (SerializationFeature[]) null));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> JsonMappers.fromJson("{}", Person.class, (DeserializationFeature) null, new DeserializationFeature[0]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.fromJson("{}", new TypeReference<Person>() {
        }, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, (DeserializationFeature[]) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JsonMappers.wrap(null));
    }

    @Test
    public void testAllPublicMethodsHaveUsageExamples() throws IOException {
        final File sourceFile = findSourceFile();
        final String source = Files.readString(sourceFile.toPath());
        final String[] lines = source.split("\r?\n");
        final List<String> methodsWithoutExamples = new ArrayList<>();

        int javadocStart = -1;
        int javadocEnd = -1;
        for (int i = 0; i < lines.length; i++) {
            final String trimmed = lines[i].trim();
            if (trimmed.equals("/**")) {
                javadocStart = i;
                javadocEnd = -1;
            } else if (javadocStart >= 0 && trimmed.equals("*/")) {
                javadocEnd = i;
            } else if (javadocEnd >= 0 && (isPublicMethodSignature(trimmed) || isMultiLineMethodContinuation(lines, i))) {
                if (isPublicMethodSignature(lines, javadocEnd, i)) {
                    final StringBuilder javadocContent = new StringBuilder();
                    for (int j = javadocStart; j <= javadocEnd; j++) {
                        javadocContent.append(lines[j]).append('\n');
                    }
                    if (!javadocContent.toString().contains("<p><b>Usage Examples:</b></p>")) {
                        methodsWithoutExamples.add("Line " + (javadocStart + 1) + ": method after javadoc missing usage examples");
                    }
                }
                javadocStart = -1;
                javadocEnd = -1;
            }
        }

        Assertions.assertTrue(methodsWithoutExamples.isEmpty(), "Methods without usage examples:\n" + String.join("\n", methodsWithoutExamples));
    }

    private static boolean isPublicMethodSignature(final String trimmed) {
        return trimmed.startsWith("public ") && trimmed.contains("(") && !trimmed.startsWith("public class ");
    }

    private static boolean isMultiLineMethodContinuation(final String[] lines, final int i) {
        final String curr = lines[i].trim();
        return !curr.isEmpty() && !curr.startsWith("//") && !curr.startsWith("/*") && !curr.startsWith("*") && !curr.startsWith("@") && curr.contains("(")
                && !curr.startsWith("public ");
    }

    private static boolean isPublicMethodSignature(final String[] lines, final int javadocEnd, final int methodStart) {
        for (int i = javadocEnd + 1; i <= methodStart; i++) {
            final String trimmed = lines[i].trim();
            if (trimmed.isEmpty() || trimmed.startsWith("@")) {
                continue;
            }
            return trimmed.startsWith("public ") && trimmed.contains("(") && !trimmed.startsWith("public class ");
        }
        return false;
    }

    private static File findSourceFile() {
        final String relativePath = "src" + File.separator + "main" + File.separator + "java" + File.separator + "com" + File.separator + "landawn"
                + File.separator + "abacus" + File.separator + "util" + File.separator + "JsonMappers.java";
        final File file = new File(relativePath);
        if (file.exists()) {
            return file;
        }
        return new File("..", relativePath);
    }
}
