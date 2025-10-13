package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.landawn.abacus.TestBase;

@Tag("2025")
public class FastJson2025Test extends TestBase {

    @TempDir
    File tempDir;

    public static class Person {
        private String name;
        private int age;

        public Person() {
        }

        public Person(String name, int age) {
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

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Person person = (Person) obj;
            return age == person.age && (name != null ? name.equals(person.name) : person.name == null);
        }

        @Override
        public int hashCode() {
            int result = name != null ? name.hashCode() : 0;
            result = 31 * result + age;
            return result;
        }
    }

    @Test
    public void test_toJson_basic() {
        Person person = new Person("John", 30);
        String json = FastJson.toJson(person);
        assertNotNull(json);
        assertTrue(json.contains("John"));
        assertTrue(json.contains("30"));
    }

    @Test
    public void test_toJson_null() {
        String json = FastJson.toJson(null);
        assertEquals("null", json);
    }

    @Test
    public void test_toJson_prettyFormat_true() {
        Person person = new Person("John", 30);
        String json = FastJson.toJson(person, true);
        assertNotNull(json);
        assertTrue(json.contains("\n") || json.contains("\t") || json.contains("  "));
    }

    @Test
    public void test_toJson_prettyFormat_false() {
        Person person = new Person("John", 30);
        String json = FastJson.toJson(person, false);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_toJson_withFeatures() {
        Person person = new Person("John", 30);
        String json = FastJson.toJson(person, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteNulls);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_toJson_withContext() {
        Person person = new Person("John", 30);
        JSONWriter.Context context = new JSONWriter.Context();
        String json = FastJson.toJson(person, context);
        assertNotNull(json);
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_toJson_toFile() throws IOException {
        Person person = new Person("John", 30);
        File file = new File(tempDir, "person.json");
        FastJson.toJson(person, file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void test_toJson_toFile_withFeatures() throws IOException {
        Person person = new Person("John", 30);
        File file = new File(tempDir, "person_features.json");
        FastJson.toJson(person, file, JSONWriter.Feature.PrettyFormat);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void test_toJson_toFile_withContext() throws IOException {
        Person person = new Person("John", 30);
        File file = new File(tempDir, "person_context.json");
        JSONWriter.Context context = new JSONWriter.Context();
        FastJson.toJson(person, file, context);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void test_toJson_toOutputStream() {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastJson.toJson(person, baos);
        assertTrue(baos.toByteArray().length > 0);
        String json = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_toJson_toOutputStream_withFeatures() {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FastJson.toJson(person, baos, JSONWriter.Feature.PrettyFormat);
        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void test_toJson_toOutputStream_withContext() {
        Person person = new Person("John", 30);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JSONWriter.Context context = new JSONWriter.Context();
        FastJson.toJson(person, baos, context);
        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void test_toJson_toWriter() throws IOException {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();
        FastJson.toJson(person, writer);
        String json = writer.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_toJson_toWriter_withFeatures() throws IOException {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();
        FastJson.toJson(person, writer, JSONWriter.Feature.PrettyFormat);
        String json = writer.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_toJson_toWriter_withContext() throws IOException {
        Person person = new Person("John", 30);
        StringWriter writer = new StringWriter();
        JSONWriter.Context context = new JSONWriter.Context();
        FastJson.toJson(person, writer, context);
        String json = writer.toString();
        assertTrue(json.contains("John"));
    }

    @Test
    public void test_fromJson_byteArray_class() {
        String jsonStr = "{\"name\":\"John\",\"age\":30}";
        byte[] jsonBytes = jsonStr.getBytes(StandardCharsets.UTF_8);
        Person person = FastJson.fromJson(jsonBytes, Person.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_byteArray_withOffset_class() {
        String jsonStr = "prefix{\"name\":\"John\",\"age\":30}suffix";
        byte[] jsonBytes = jsonStr.getBytes(Charsets.UTF_8);
        N.println(new String(jsonBytes, 6, 24, Charsets.UTF_8));
        Person person = FastJson.fromJson(jsonBytes, 6, 24, Person.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_string_class() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Person person = FastJson.fromJson(json, Person.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_string_class_null() {
        String json = "null";
        Person person = FastJson.fromJson(json, Person.class);
        assertNull(person);
    }

    @Test
    public void test_fromJson_string_class_withFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Person person = FastJson.fromJson(json, Person.class, JSONReader.Feature.SupportSmartMatch);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_string_class_withContext() {
        String json = "{\"name\":\"John\",\"age\":30}";
        JSONReader.Context context = new JSONReader.Context();
        Person person = FastJson.fromJson(json, Person.class, context);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_string_type() {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        Type listType = new TypeReference<List<Person>>() {
        }.getType();
        List<Person> people = FastJson.fromJson(json, listType);
        assertNotNull(people);
        assertEquals(2, people.size());
        assertEquals("John", people.get(0).getName());
        assertEquals("Jane", people.get(1).getName());
    }

    @Test
    public void test_fromJson_string_type_withFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Type listType = new TypeReference<List<Person>>() {
        }.getType();
        List<Person> people = FastJson.fromJson(json, listType, JSONReader.Feature.SupportSmartMatch);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_fromJson_string_type_withContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Type listType = new TypeReference<List<Person>>() {
        }.getType();
        JSONReader.Context context = new JSONReader.Context();
        List<Person> people = FastJson.fromJson(json, listType, context);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_fromJson_string_typeReference() {
        String json = "[{\"name\":\"John\",\"age\":30},{\"name\":\"Jane\",\"age\":25}]";
        TypeReference<List<Person>> typeRef = new TypeReference<List<Person>>() {
        };
        List<Person> people = FastJson.fromJson(json, typeRef);
        assertNotNull(people);
        assertEquals(2, people.size());
        assertEquals("John", people.get(0).getName());
        assertEquals("Jane", people.get(1).getName());
    }

    @Test
    public void test_fromJson_string_typeReference_withFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        TypeReference<List<Person>> typeRef = new TypeReference<List<Person>>() {
        };
        List<Person> people = FastJson.fromJson(json, typeRef, JSONReader.Feature.SupportSmartMatch);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_fromJson_string_typeReference_withContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        TypeReference<List<Person>> typeRef = new TypeReference<List<Person>>() {
        };
        JSONReader.Context context = new JSONReader.Context();
        List<Person> people = FastJson.fromJson(json, typeRef, context);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_fromJson_reader_class() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Reader reader = new StringReader(json);
        Person person = FastJson.fromJson(reader, Person.class);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_reader_class_withFeatures() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Reader reader = new StringReader(json);
        Person person = FastJson.fromJson(reader, Person.class, JSONReader.Feature.SupportSmartMatch);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_reader_class_withContext() {
        String json = "{\"name\":\"John\",\"age\":30}";
        Reader reader = new StringReader(json);
        JSONReader.Context context = new JSONReader.Context();
        Person person = FastJson.fromJson(reader, Person.class, context);
        assertNotNull(person);
        assertEquals("John", person.getName());
        assertEquals(30, person.getAge());
    }

    @Test
    public void test_fromJson_reader_type() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Reader reader = new StringReader(json);
        Type listType = new TypeReference<List<Person>>() {
        }.getType();
        List<Person> people = FastJson.fromJson(reader, listType);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_fromJson_reader_type_withFeatures() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Reader reader = new StringReader(json);
        Type listType = new TypeReference<List<Person>>() {
        }.getType();
        List<Person> people = FastJson.fromJson(reader, listType, JSONReader.Feature.SupportSmartMatch);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_fromJson_reader_type_withContext() {
        String json = "[{\"name\":\"John\",\"age\":30}]";
        Reader reader = new StringReader(json);
        Type listType = new TypeReference<List<Person>>() {
        }.getType();
        JSONReader.Context context = new JSONReader.Context();
        List<Person> people = FastJson.fromJson(reader, listType, context);
        assertNotNull(people);
        assertEquals(1, people.size());
    }

    @Test
    public void test_roundTrip() {
        Person original = new Person("Alice", 28);
        String json = FastJson.toJson(original);
        Person restored = FastJson.fromJson(json, Person.class);
        assertEquals(original, restored);
    }

    @Test
    public void test_roundTrip_list() {
        List<Person> original = new ArrayList<>();
        original.add(new Person("Alice", 28));
        original.add(new Person("Bob", 35));
        String json = FastJson.toJson(original);
        TypeReference<List<Person>> typeRef = new TypeReference<List<Person>>() {
        };
        List<Person> restored = FastJson.fromJson(json, typeRef);
        assertEquals(original.size(), restored.size());
        assertEquals(original.get(0), restored.get(0));
        assertEquals(original.get(1), restored.get(1));
    }

    @Test
    public void test_roundTrip_map() {
        Map<String, Integer> original = new HashMap<>();
        original.put("one", 1);
        original.put("two", 2);
        String json = FastJson.toJson(original);
        TypeReference<Map<String, Integer>> typeRef = new TypeReference<Map<String, Integer>>() {
        };
        Map<String, Integer> restored = FastJson.fromJson(json, typeRef);
        assertEquals(original.size(), restored.size());
        assertEquals(original.get("one"), restored.get("one"));
        assertEquals(original.get("two"), restored.get("two"));
    }

    @Test
    public void test_toJson_toFile_roundTrip() throws IOException {
        Person person = new Person("Charlie", 42);
        File file = new File(tempDir, "roundtrip.json");
        FastJson.toJson(person, file);

        String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        Person restored = FastJson.fromJson(json, Person.class);
        assertEquals(person, restored);
    }

    @Test
    public void test_complexObject() {
        Map<String, Object> complex = new HashMap<>();
        complex.put("string", "value");
        complex.put("number", 123);
        complex.put("boolean", true);
        complex.put("null", null);
        List<Integer> numbers = new ArrayList<>();
        numbers.add(1);
        numbers.add(2);
        numbers.add(3);
        complex.put("list", numbers);

        String json = FastJson.toJson(complex);
        assertNotNull(json);
        assertTrue(json.contains("value"));
        assertTrue(json.contains("123"));
        assertTrue(json.contains("true"));
    }

    @Test
    public void test_prettyFormat_difference() {
        Person person = new Person("Test", 20);
        String compact = FastJson.toJson(person, false);
        String pretty = FastJson.toJson(person, true);
        assertTrue(pretty.length() >= compact.length());
    }

    @Test
    public void test_emptyList() {
        List<Person> emptyList = new ArrayList<>();
        String json = FastJson.toJson(emptyList);
        assertEquals("[]", json);

        TypeReference<List<Person>> typeRef = new TypeReference<List<Person>>() {
        };
        List<Person> restored = FastJson.fromJson(json, typeRef);
        assertNotNull(restored);
        assertEquals(0, restored.size());
    }

    @Test
    public void test_emptyMap() {
        Map<String, Object> emptyMap = new HashMap<>();
        String json = FastJson.toJson(emptyMap);
        assertEquals("{}", json);

        TypeReference<Map<String, Object>> typeRef = new TypeReference<Map<String, Object>>() {
        };
        Map<String, Object> restored = FastJson.fromJson(json, typeRef);
        assertNotNull(restored);
        assertEquals(0, restored.size());
    }
}
