package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.landawn.abacus.TestBase;

public class FastJsonTest extends TestBase {

    @TempDir
    Path tempDir;

    private TestPerson testPerson;

    public static class ExplodingBean {
        public String getValue() {
            throw new AssertionError("serialization must not start for an invalid destination");
        }
    }

    public static class TestPerson {
        private String name;
        private int age;
        private String email;

        public TestPerson() {
        }

        public TestPerson(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public TestPerson(final String name, final int age, final String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final TestPerson that = (TestPerson) obj;
            return age == that.age && Objects.equals(name, that.name) && Objects.equals(email, that.email);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age, email);
        }
    }

    private static Writer explodingWriter() {
        return new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("Test exception");
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
    }

    @BeforeEach
    public void setUp() {
        testPerson = new TestPerson("John", 30, "john@example.com");
    }

    @Test
    public void testToJson() {
        assertEquals("\"test\"", FastJson.toJson("test"));
        assertEquals("42", FastJson.toJson(42));
        assertEquals("[\"a\",\"b\",\"c\"]", FastJson.toJson(Arrays.asList("a", "b", "c")));
        assertEquals("true", FastJson.toJson(true));
        assertEquals("false", FastJson.toJson(false));
        assertEquals("null", FastJson.toJson(null));
        assertEquals("{}", FastJson.toJson(new HashMap<>()));
        assertEquals("[]", FastJson.toJson(Collections.emptyList()));

        final String json = FastJson.toJson(testPerson);
        assertTrue(json.contains("\"name\":\"John\""));
        assertTrue(json.contains("\"age\":30"));
        assertTrue(json.contains("\"email\":\"john@example.com\""));
        assertEquals(testPerson, FastJson.fromJson(json, TestPerson.class));
    }

    @Test
    public void testToJson_PrettyFormat() {
        final String pretty = FastJson.toJson(testPerson, true);
        assertTrue(pretty.contains("\n"));
        assertTrue(pretty.contains("\t"));
        assertTrue(pretty.contains("\"name\":\"John\""));

        final String compact = FastJson.toJson(testPerson, false);
        assertFalse(compact.contains("\n"));
        assertFalse(compact.contains("\t"));
        assertEquals("null", FastJson.toJson(null, true));
    }

    @Test
    public void testToJson_Features() {
        assertTrue(FastJson.toJson(testPerson, JSONWriter.Feature.PrettyFormat).contains("\n"));
        assertEquals("null", FastJson.toJson(null, JSONWriter.Feature.PrettyFormat));
        assertTrue(FastJson.toJson(testPerson, new JSONWriter.Feature[0]).contains("\"name\":\"John\""));

        final String withNulls = FastJson.toJson(new TestPerson("Jane", 25, null), JSONWriter.Feature.WriteNulls, JSONWriter.Feature.PrettyFormat);
        assertTrue(withNulls.contains("\"email\":null"));
        assertTrue(withNulls.contains("\n"));
    }

    @Test
    public void testToJson_Context() {
        final JSONWriter.Context context = new JSONWriter.Context();
        context.setDateFormat("yyyy-MM-dd");
        assertTrue(FastJson.toJson(testPerson, context).contains("\"name\":\"John\""));
        assertEquals("null", FastJson.toJson(null, context));
    }

    @Test
    public void testToJson_File() throws Exception {
        final File outputFile = tempDir.resolve("test.json").toFile();
        FastJson.toJson(testPerson, outputFile);
        final String content = Files.readString(outputFile.toPath());
        assertTrue(content.contains("\"name\":\"John\""));
        assertTrue(content.contains("\"age\":30"));

        final File nullFile = tempDir.resolve("null.json").toFile();
        FastJson.toJson(null, nullFile);
        assertEquals("null", Files.readString(nullFile.toPath()));

        final File prettyFile = tempDir.resolve("pretty.json").toFile();
        FastJson.toJson(testPerson, prettyFile, JSONWriter.Feature.PrettyFormat);
        assertTrue(Files.readString(prettyFile.toPath()).contains("\n"));

        final File nullsFile = tempDir.resolve("nulls.json").toFile();
        FastJson.toJson(new TestPerson("Jane", 25, null), nullsFile, JSONWriter.Feature.WriteNulls);
        assertTrue(Files.readString(nullsFile.toPath()).contains("\"email\":null"));

        final File contextFile = tempDir.resolve("context.json").toFile();
        FastJson.toJson(testPerson, contextFile, new JSONWriter.Context());
        assertTrue(Files.readString(contextFile.toPath()).contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_OutputStream() {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        FastJson.toJson(testPerson, os);
        assertTrue(os.toString().contains("\"name\":\"John\""));

        final ByteArrayOutputStream nullOs = new ByteArrayOutputStream();
        FastJson.toJson(null, nullOs);
        assertEquals("null", nullOs.toString());

        final ByteArrayOutputStream emptyOs = new ByteArrayOutputStream();
        FastJson.toJson(new HashMap<>(), emptyOs);
        assertEquals("{}", emptyOs.toString());

        final ByteArrayOutputStream features = new ByteArrayOutputStream();
        FastJson.toJson(testPerson, features, JSONWriter.Feature.PrettyFormat);
        assertTrue(features.toString().contains("\n"));

        final ByteArrayOutputStream nulls = new ByteArrayOutputStream();
        FastJson.toJson(new TestPerson("Jane", 25, null), nulls, JSONWriter.Feature.WriteNulls);
        assertTrue(nulls.toString().contains("\"email\":null"));

        final ByteArrayOutputStream context = new ByteArrayOutputStream();
        FastJson.toJson(testPerson, context, new JSONWriter.Context());
        assertTrue(context.toString().contains("\"name\":\"John\""));

        final ByteArrayOutputStream nullContext = new ByteArrayOutputStream();
        FastJson.toJson(null, nullContext, new JSONWriter.Context());
        assertEquals("null", nullContext.toString());
    }

    @Test
    public void testToJson_Writer() {
        final StringWriter writer = new StringWriter();
        FastJson.toJson(testPerson, writer);
        assertTrue(writer.toString().contains("\"name\":\"John\""));

        final StringWriter nullWriter = new StringWriter();
        FastJson.toJson(null, nullWriter);
        assertEquals("null", nullWriter.toString());

        final StringWriter features = new StringWriter();
        FastJson.toJson(testPerson, features, JSONWriter.Feature.PrettyFormat);
        assertTrue(features.toString().contains("\n"));

        final StringWriter context = new StringWriter();
        FastJson.toJson(testPerson, context, new JSONWriter.Context());
        assertTrue(context.toString().contains("\"name\":\"John\""));
    }

    @Test
    public void testToJson_Writer_IOException() {
        assertThrows(RuntimeException.class, () -> FastJson.toJson(testPerson, explodingWriter()));
        assertThrows(RuntimeException.class, () -> FastJson.toJson(testPerson, explodingWriter(), JSONWriter.Feature.PrettyFormat));
        assertThrows(RuntimeException.class, () -> FastJson.toJson(testPerson, explodingWriter(), new JSONWriter.Context()));
    }

    @Test
    public void testRoundTrip() throws Exception {
        assertEquals(testPerson, FastJson.fromJson(FastJson.toJson(testPerson), TestPerson.class));

        final List<TestPerson> people = Arrays.asList(new TestPerson("John", 30), new TestPerson("Jane", 25), new TestPerson("Bob", 35));
        assertEquals(people, FastJson.fromJson(FastJson.toJson(people), new TypeReference<List<TestPerson>>() {
        }));

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", "test");
        map.put("count", 42);
        final Map<String, Object> restored = FastJson.fromJson(FastJson.toJson(map), new TypeReference<Map<String, Object>>() {
        });
        assertEquals("test", restored.get("name"));
        assertEquals(42, restored.get("count"));

        final File jsonFile = tempDir.resolve("roundtrip.json").toFile();
        FastJson.toJson(testPerson, jsonFile);
        try (FileReader reader = new FileReader(jsonFile)) {
            assertEquals(testPerson, FastJson.fromJson(reader, TestPerson.class));
        }

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        FastJson.toJson(testPerson, os);
        assertEquals(testPerson, FastJson.fromJson(os.toByteArray(), TestPerson.class));
    }

    @Test
    public void testFromJson() {
        final String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";
        assertEquals(testPerson, FastJson.fromJson(json, TestPerson.class));
        assertNull(FastJson.fromJson("null", TestPerson.class));
        assertEquals(Integer.valueOf(42), FastJson.fromJson("42", Integer.class));
        assertEquals(Boolean.TRUE, FastJson.fromJson("true", Boolean.class));
        assertEquals(Boolean.FALSE, FastJson.fromJson("false", Boolean.class));

        final List<?> list = FastJson.fromJson("[\"a\",\"b\",\"c\"]", List.class);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));

        final TestPerson empty = FastJson.fromJson("{}", TestPerson.class);
        assertNull(empty.getName());
        assertEquals(0, empty.getAge());

        assertEquals("John", FastJson.fromJson(json, TestPerson.class, JSONReader.Feature.SupportSmartMatch).getName());
        assertNull(FastJson.fromJson("null", TestPerson.class, JSONReader.Feature.SupportSmartMatch));
        assertEquals("John", FastJson.fromJson(json, TestPerson.class, new JSONReader.Feature[0]).getName());
        assertEquals("John", FastJson.fromJson(json, TestPerson.class, new JSONReader.Context()).getName());
        assertNull(FastJson.fromJson("null", TestPerson.class, new JSONReader.Context()));
    }

    @Test
    public void testFromJson_ByteArray() {
        final String json = "{\"name\":\"John\",\"age\":30,\"email\":\"john@example.com\"}";
        assertEquals(testPerson, FastJson.fromJson(json.getBytes(StandardCharsets.UTF_8), TestPerson.class));
        assertNull(FastJson.fromJson("null".getBytes(StandardCharsets.UTF_8), TestPerson.class));

        final TestPerson empty = FastJson.fromJson("{}".getBytes(StandardCharsets.UTF_8), TestPerson.class);
        assertNull(empty.getName());

        @SuppressWarnings("rawtypes")
        final Map map = FastJson.fromJson("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8), Map.class);
        assertEquals("value", map.get("key"));

        final String full = "prefix{\"name\":\"Jane\",\"age\":25}suffix";
        final TestPerson offset = FastJson.fromJson(full.getBytes(StandardCharsets.UTF_8), 6, 24, TestPerson.class);
        assertEquals("Jane", offset.getName());
        assertEquals(25, offset.getAge());
        assertNull(FastJson.fromJson("prefixnullsuffix".getBytes(StandardCharsets.UTF_8), 6, 4, TestPerson.class));

        final byte[] bob = "{\"name\":\"Bob\",\"age\":40}".getBytes(StandardCharsets.UTF_8);
        assertEquals("Bob", FastJson.fromJson(bob, 0, bob.length, TestPerson.class).getName());
    }

    @Test
    public void testFromJson_Type() {
        final Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();
        final List<TestPerson> people = FastJson.fromJson("[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]", listType);
        assertEquals(2, people.size());
        assertEquals("Jane", people.get(1).getName());
        assertNull(FastJson.<List<TestPerson>> fromJson("null", listType));

        final Type mapType = new TypeReference<Map<String, Integer>>() {
        }.getType();
        final Map<String, Integer> map = FastJson.fromJson("{\"a\":1,\"b\":2}", mapType);
        assertEquals(1, map.get("a"));

        final List<TestPerson> withFeatures = FastJson.fromJson("[{\"name\":\"John\",\"age\":30}]", listType, JSONReader.Feature.SupportSmartMatch);
        assertEquals("John", withFeatures.get(0).getName());
        assertNull(FastJson.<List<TestPerson>> fromJson("null", listType, JSONReader.Feature.SupportSmartMatch));
        final List<TestPerson> withContext = FastJson.fromJson("[{\"name\":\"John\",\"age\":30}]", listType, new JSONReader.Context());
        assertEquals("John", withContext.get(0).getName());
        assertNull(FastJson.<List<TestPerson>> fromJson("null", listType, new JSONReader.Context()));
    }

    @Test
    public void testFromJson_TypeReference() {
        final TypeReference<List<TestPerson>> listType = new TypeReference<>() {
        };
        assertEquals("John", FastJson.fromJson("[{\"name\":\"John\",\"age\":30}]", listType).get(0).getName());
        assertNull(FastJson.fromJson("null", listType));

        final TypeReference<Map<String, String>> mapType = new TypeReference<>() {
        };
        assertEquals("v", FastJson.fromJson("{\"k\":\"v\"}", mapType).get("k"));

        final TypeReference<Map<String, Map<String, Integer>>> nested = new TypeReference<>() {
        };
        assertEquals(1, FastJson.fromJson("{\"outer\":{\"inner\":1}}", nested).get("outer").get("inner"));

        assertEquals("John", FastJson.fromJson("[{\"name\":\"John\",\"age\":30}]", listType, JSONReader.Feature.SupportSmartMatch).get(0).getName());
        assertNull(FastJson.fromJson("null", listType, JSONReader.Feature.SupportSmartMatch));
        assertEquals("John", FastJson.fromJson("[{\"name\":\"John\",\"age\":30}]", listType, new JSONReader.Context()).get(0).getName());
        assertNull(FastJson.fromJson("null", listType, new JSONReader.Context()));
    }

    @Test
    public void testFromJson_Reader() throws Exception {
        assertEquals(testPerson, FastJson.fromJson(new StringReader(FastJson.toJson(testPerson)), TestPerson.class));
        assertNull(FastJson.fromJson(new StringReader("null"), TestPerson.class));
        assertEquals("John",
                FastJson.fromJson(new StringReader("{\"name\":\"John\",\"age\":30}"), TestPerson.class, JSONReader.Feature.SupportSmartMatch).getName());
        assertNull(FastJson.fromJson(new StringReader("null"), TestPerson.class, JSONReader.Feature.SupportSmartMatch));
        assertEquals("John", FastJson.fromJson(new StringReader("{\"name\":\"John\",\"age\":30}"), TestPerson.class, new JSONReader.Context()).getName());
        assertNull(FastJson.fromJson(new StringReader("null"), TestPerson.class, new JSONReader.Context()));

        final Type listType = new TypeReference<List<TestPerson>>() {
        }.getType();
        final List<TestPerson> fromReader = FastJson.fromJson(new StringReader("[{\"name\":\"John\",\"age\":30}]"), listType);
        assertEquals("John", fromReader.get(0).getName());
        assertNull(FastJson.<List<TestPerson>> fromJson(new StringReader("null"), listType));
        final List<TestPerson> withFeatures = FastJson.fromJson(new StringReader("[{\"name\":\"John\",\"age\":30}]"), listType,
                JSONReader.Feature.SupportSmartMatch);
        assertEquals("John", withFeatures.get(0).getName());
        assertNull(FastJson.<List<TestPerson>> fromJson(new StringReader("null"), listType, JSONReader.Feature.SupportSmartMatch));
        final List<TestPerson> withContext = FastJson.fromJson(new StringReader("[{\"name\":\"John\",\"age\":30}]"), listType, new JSONReader.Context());
        assertEquals("John", withContext.get(0).getName());
        assertNull(FastJson.<List<TestPerson>> fromJson(new StringReader("null"), listType, new JSONReader.Context()));
    }

    @Test
    public void testByteArraySegmentValidation() {
        final byte[] json = "xx{\"name\":\"Ada\",\"age\":37}yy".getBytes(StandardCharsets.UTF_8);
        assertEquals("Ada", FastJson.fromJson(json, 2, json.length - 4, TestPerson.class).getName());
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson((byte[]) null, 0, 0, TestPerson.class));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson(json, 0, -1, TestPerson.class));
        assertThrows(IndexOutOfBoundsException.class, () -> FastJson.fromJson(json, -1, 1, TestPerson.class));
        assertThrows(IndexOutOfBoundsException.class, () -> FastJson.fromJson(json, json.length, 1, TestPerson.class));
        assertThrows(IndexOutOfBoundsException.class, () -> FastJson.fromJson(json, 1, Integer.MAX_VALUE, TestPerson.class));
    }

    @Test
    public void testRequiredConfigurationAndDestinationArgumentsAreValidatedEagerly() {
        assertThrows(IllegalArgumentException.class, () -> FastJson.toJson(new ExplodingBean(), (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.toJson(testPerson, (java.io.OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.toJson(testPerson, (File) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.toJson(testPerson, (JSONWriter.Context) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.toJson(testPerson, (JSONWriter.Feature[]) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson("{}", (Class<TestPerson>) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson("{}", (Type) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson("{}", (TypeReference<TestPerson>) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson("{}", TestPerson.class, (JSONReader.Context) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson(new StringReader("{}"), TestPerson.class, (JSONReader.Feature[]) null));
        assertThrows(IllegalArgumentException.class, () -> FastJson.fromJson((Reader) null, TestPerson.class));
    }
}
