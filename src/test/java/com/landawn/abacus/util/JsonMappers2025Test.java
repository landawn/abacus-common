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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.landawn.abacus.TestBase;

@Tag("2025")
public class JsonMappers2025Test extends TestBase {

    public static class Person {
        public String name;
        public int age;

        public Person() {
        }

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            Person person = (Person) o;
            return age == person.age && name.equals(person.name);
        }

        @Override
        public int hashCode() {
            return name.hashCode() + age;
        }
    }

    @TempDir
    File tempDir;

    @Test
    public void testToJson_Object() {
        Person person = new Person("John", 30);
        String json = JsonMappers.toJson(person);
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("John"));
        Assertions.assertTrue(json.contains("30"));

        String nullJson = JsonMappers.toJson(null);
        Assertions.assertEquals("null", nullJson);
    }

    @Test
    public void testToJson_ObjectWithPrettyFormat() {
        Person person = new Person("Alice", 25);

        String compact = JsonMappers.toJson(person, false);
        Assertions.assertNotNull(compact);
        Assertions.assertFalse(compact.contains("\n"));

        String pretty = JsonMappers.toJson(person, true);
        Assertions.assertNotNull(pretty);
        Assertions.assertTrue(pretty.contains("\n") || pretty.contains("  "));
    }

    @Test
    public void testToJson_ObjectWithSerializationFeatures() {
        Map<String, Object> data = new HashMap<>();
        data.put("key", "value");

        String json = JsonMappers.toJson(data, SerializationFeature.INDENT_OUTPUT);
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("\n") || json.length() > 20);
    }

    @Test
    public void testToJson_ObjectWithSerializationConfig() {
        Person person = new Person("Bob", 40);
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        String json = JsonMappers.toJson(person, config);
        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("Bob"));
    }

    @Test
    public void testToJson_ObjectToFile() throws IOException {
        Person person = new Person("Charlie", 35);
        File outputFile = new File(tempDir, "person.json");

        JsonMappers.toJson(person, outputFile);

        Assertions.assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        Assertions.assertTrue(content.contains("Charlie"));
    }

    @Test
    public void testToJson_ObjectToFileWithConfig() throws IOException {
        Person person = new Person("Diana", 28);
        File outputFile = new File(tempDir, "person-pretty.json");
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, outputFile, config);

        Assertions.assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath());
        Assertions.assertTrue(content.contains("Diana"));
    }

    @Test
    public void testToJson_ObjectToOutputStream() throws IOException {
        Person person = new Person("Eve", 32);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        JsonMappers.toJson(person, baos);

        String json = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(json.contains("Eve"));
        Assertions.assertTrue(json.contains("32"));
    }

    @Test
    public void testToJson_ObjectToOutputStreamWithConfig() throws IOException {
        Person person = new Person("Frank", 45);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, baos, config);

        String json = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(json.contains("Frank"));
    }

    @Test
    public void testToJson_ObjectToWriter() throws IOException {
        Person person = new Person("Grace", 29);
        StringWriter writer = new StringWriter();

        JsonMappers.toJson(person, writer);

        String json = writer.toString();
        Assertions.assertTrue(json.contains("Grace"));
        Assertions.assertTrue(json.contains("29"));
    }

    @Test
    public void testToJson_ObjectToWriterWithConfig() throws IOException {
        Person person = new Person("Henry", 50);
        StringWriter writer = new StringWriter();
        SerializationConfig config = JsonMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);

        JsonMappers.toJson(person, writer, config);

        String json = writer.toString();
        Assertions.assertTrue(json.contains("Henry"));
    }

    @Test
    public void testToJson_ObjectToDataOutput() throws IOException {
        Person person = new Person("Ivy", 27);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(baos);

        JsonMappers.toJson(person, dos);

        String json = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(json.contains("Ivy"));
    }

    @Test
    public void testToJson_ObjectToDataOutputWithConfig() throws IOException {
        Person person = new Person("Jack", 33);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(baos);
        SerializationConfig config = JsonMappers.createSerializationConfig();

        JsonMappers.toJson(person, dos, config);

        String json = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(json.contains("Jack"));
    }

    @Test
    public void testFromJson_ByteArray() {
        String jsonString = "{\"name\":\"Kate\",\"age\":26}";
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        Person person = JsonMappers.fromJson(jsonBytes, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Kate", person.name);
        Assertions.assertEquals(26, person.age);
    }

    @Test
    public void testFromJson_ByteArrayWithOffset() {
        String prefix = "XXXX";
        String jsonString = "{\"name\":\"Leo\",\"age\":31}";
        String suffix = "YYYY";
        byte[] buffer = (prefix + jsonString + suffix).getBytes(StandardCharsets.UTF_8);

        Person person = JsonMappers.fromJson(buffer, prefix.length(), jsonString.length(), Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Leo", person.name);
        Assertions.assertEquals(31, person.age);
    }

    @Test
    public void testFromJson_String() {
        String json = "{\"name\":\"Mary\",\"age\":24}";

        Person person = JsonMappers.fromJson(json, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Mary", person.name);
        Assertions.assertEquals(24, person.age);

        Person nullPerson = JsonMappers.fromJson("null", Person.class);
        Assertions.assertNull(nullPerson);
    }

    @Test
    public void testFromJson_StringWithDeserializationFeatures() {
        String json = "{\"name\":\"Nancy\",\"age\":22,\"unknown\":\"field\"}";

        Person person = JsonMappers.fromJson(json, Person.class,
                JsonMappers.createDeserializationConfig().without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES));

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Nancy", person.name);
    }

    @Test
    public void testFromJson_StringWithDeserializationConfig() {
        String json = "{\"name\":\"Oscar\",\"age\":38}";
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(json, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Oscar", person.name);
        Assertions.assertEquals(38, person.age);
    }

    @Test
    public void testFromJson_File() throws IOException {
        File jsonFile = new File(tempDir, "test-person.json");
        String json = "{\"name\":\"Paul\",\"age\":42}";
        Files.writeString(jsonFile.toPath(), json);

        Person person = JsonMappers.fromJson(jsonFile, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Paul", person.name);
        Assertions.assertEquals(42, person.age);
    }

    @Test
    public void testFromJson_FileWithConfig() throws IOException {
        File jsonFile = new File(tempDir, "test-person-config.json");
        String json = "{\"name\":\"Quinn\",\"age\":36}";
        Files.writeString(jsonFile.toPath(), json);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(jsonFile, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Quinn", person.name);
    }

    @Test
    public void testFromJson_InputStream() throws IOException {
        String json = "{\"name\":\"Rachel\",\"age\":29}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        Person person = JsonMappers.fromJson(bais, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Rachel", person.name);
        Assertions.assertEquals(29, person.age);
    }

    @Test
    public void testFromJson_InputStreamWithConfig() throws IOException {
        String json = "{\"name\":\"Sam\",\"age\":34}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(bais, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Sam", person.name);
    }

    @Test
    public void testFromJson_Reader() throws IOException {
        String json = "{\"name\":\"Tina\",\"age\":27}";
        StringReader reader = new StringReader(json);

        Person person = JsonMappers.fromJson(reader, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Tina", person.name);
        Assertions.assertEquals(27, person.age);
    }

    @Test
    public void testFromJson_ReaderWithConfig() throws IOException {
        String json = "{\"name\":\"Uma\",\"age\":41}";
        StringReader reader = new StringReader(json);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(reader, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Uma", person.name);
    }

    @Test
    public void testFromJson_URL() throws IOException {
        File jsonFile = new File(tempDir, "url-test.json");
        String json = "{\"name\":\"Victor\",\"age\":39}";
        Files.writeString(jsonFile.toPath(), json);
        URL url = jsonFile.toURI().toURL();

        Person person = JsonMappers.fromJson(url, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Victor", person.name);
    }

    @Test
    public void testFromJson_URLWithConfig() throws IOException {
        File jsonFile = new File(tempDir, "url-config-test.json");
        String json = "{\"name\":\"Wendy\",\"age\":23}";
        Files.writeString(jsonFile.toPath(), json);
        URL url = jsonFile.toURI().toURL();
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(url, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Wendy", person.name);
    }

    @Test
    public void testFromJson_DataInput() throws IOException {
        String json = "{\"name\":\"Xavier\",\"age\":44}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DataInput dis = new DataInputStream(bais);

        Person person = JsonMappers.fromJson(dis, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Xavier", person.name);
    }

    @Test
    public void testFromJson_DataInputWithConfig() throws IOException {
        String json = "{\"name\":\"Yara\",\"age\":26}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DataInput dis = new DataInputStream(bais);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        Person person = JsonMappers.fromJson(dis, Person.class, config);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Yara", person.name);
    }

    @Test
    public void testFromJson_ByteArrayWithTypeReference() {
        String jsonString = "[{\"name\":\"Zoe\",\"age\":30},{\"name\":\"Adam\",\"age\":35}]";
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        List<Person> people = JsonMappers.fromJson(jsonBytes, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(2, people.size());
        Assertions.assertEquals("Zoe", people.get(0).name);
    }

    @Test
    public void testFromJson_ByteArrayWithOffsetAndTypeReference() {
        String prefix = "XXX";
        String jsonString = "[{\"name\":\"Ben\",\"age\":28}]";
        String suffix = "YYY";
        byte[] buffer = (prefix + jsonString + suffix).getBytes(StandardCharsets.UTF_8);

        List<Person> people = JsonMappers.fromJson(buffer, prefix.length(), jsonString.length(), new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
        Assertions.assertEquals("Ben", people.get(0).name);
    }

    @Test
    public void testFromJson_StringWithTypeReference() {
        String json = "[{\"name\":\"Carol\",\"age\":31},{\"name\":\"David\",\"age\":33}]";

        List<Person> people = JsonMappers.fromJson(json, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(2, people.size());
        Assertions.assertEquals("Carol", people.get(0).name);
        Assertions.assertEquals("David", people.get(1).name);
    }

    @Test
    public void testFromJson_StringWithTypeReferenceAndFeatures() {
        String json = "[{\"name\":\"Emma\",\"age\":25}]";

        List<Person> people = JsonMappers.fromJson(json, new TypeReference<List<Person>>() {
        }, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
    }

    @Test
    public void testFromJson_StringWithTypeReferenceAndConfig() {
        String json = "[{\"name\":\"Fiona\",\"age\":37}]";
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<Person> people = JsonMappers.fromJson(json, new TypeReference<List<Person>>() {
        }, config);

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
        Assertions.assertEquals("Fiona", people.get(0).name);
    }

    @Test
    public void testFromJson_FileWithTypeReference() throws IOException {
        File jsonFile = new File(tempDir, "people-list.json");
        String json = "[{\"name\":\"George\",\"age\":40}]";
        Files.writeString(jsonFile.toPath(), json);

        List<Person> people = JsonMappers.fromJson(jsonFile, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
        Assertions.assertEquals("George", people.get(0).name);
    }

    @Test
    public void testFromJson_FileWithTypeReferenceAndConfig() throws IOException {
        File jsonFile = new File(tempDir, "people-config.json");
        String json = "[{\"name\":\"Hannah\",\"age\":28}]";
        Files.writeString(jsonFile.toPath(), json);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<Person> people = JsonMappers.fromJson(jsonFile, new TypeReference<List<Person>>() {
        }, config);

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Hannah", people.get(0).name);
    }

    @Test
    public void testFromJson_InputStreamWithTypeReference() throws IOException {
        String json = "[{\"name\":\"Ian\",\"age\":32}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        List<Person> people = JsonMappers.fromJson(bais, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
        Assertions.assertEquals("Ian", people.get(0).name);
    }

    @Test
    public void testFromJson_InputStreamWithTypeReferenceAndConfig() throws IOException {
        String json = "[{\"name\":\"Julia\",\"age\":29}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<Person> people = JsonMappers.fromJson(bais, new TypeReference<List<Person>>() {
        }, config);

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Julia", people.get(0).name);
    }

    @Test
    public void testFromJson_ReaderWithTypeReference() throws IOException {
        String json = "[{\"name\":\"Kevin\",\"age\":35}]";
        StringReader reader = new StringReader(json);

        List<Person> people = JsonMappers.fromJson(reader, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
        Assertions.assertEquals("Kevin", people.get(0).name);
    }

    @Test
    public void testFromJson_ReaderWithTypeReferenceAndConfig() throws IOException {
        String json = "[{\"name\":\"Laura\",\"age\":27}]";
        StringReader reader = new StringReader(json);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<Person> people = JsonMappers.fromJson(reader, new TypeReference<List<Person>>() {
        }, config);

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Laura", people.get(0).name);
    }

    @Test
    public void testFromJson_URLWithTypeReference() throws IOException {
        File jsonFile = new File(tempDir, "url-type-ref.json");
        String json = "[{\"name\":\"Mike\",\"age\":43}]";
        Files.writeString(jsonFile.toPath(), json);
        URL url = jsonFile.toURI().toURL();

        List<Person> people = JsonMappers.fromJson(url, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Mike", people.get(0).name);
    }

    @Test
    public void testFromJson_URLWithTypeReferenceAndConfig() throws IOException {
        File jsonFile = new File(tempDir, "url-type-config.json");
        String json = "[{\"name\":\"Nina\",\"age\":24}]";
        Files.writeString(jsonFile.toPath(), json);
        URL url = jsonFile.toURI().toURL();
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<Person> people = JsonMappers.fromJson(url, new TypeReference<List<Person>>() {
        }, config);

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Nina", people.get(0).name);
    }

    @Test
    public void testFromJson_DataInputWithTypeReference() throws IOException {
        String json = "[{\"name\":\"Oliver\",\"age\":38}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DataInput dis = new DataInputStream(bais);

        List<Person> people = JsonMappers.fromJson(dis, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Oliver", people.get(0).name);
    }

    @Test
    public void testFromJson_DataInputWithTypeReferenceAndConfig() throws IOException {
        String json = "[{\"name\":\"Pam\",\"age\":31}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DataInput dis = new DataInputStream(bais);
        DeserializationConfig config = JsonMappers.createDeserializationConfig();

        List<Person> people = JsonMappers.fromJson(dis, new TypeReference<List<Person>>() {
        }, config);

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Pam", people.get(0).name);
    }

    @Test
    public void testCreateSerializationConfig() {
        SerializationConfig config = JsonMappers.createSerializationConfig();
        Assertions.assertNotNull(config);
    }

    @Test
    public void testCreateDeserializationConfig() {
        DeserializationConfig config = JsonMappers.createDeserializationConfig();
        Assertions.assertNotNull(config);
    }

    @Test
    public void testWrap() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One wrapper = JsonMappers.wrap(mapper);
        Assertions.assertNotNull(wrapper);
    }

    @Test
    public void testOne_ToJson_Object() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        Person person = new Person("Alice", 25);
        String json = one.toJson(person);

        Assertions.assertNotNull(json);
        Assertions.assertTrue(json.contains("Alice"));
    }

    @Test
    public void testOne_ToJson_ObjectWithPrettyFormat() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        Person person = new Person("Bob", 30);
        String pretty = one.toJson(person, true);
        String compact = one.toJson(person, false);

        Assertions.assertNotNull(pretty);
        Assertions.assertNotNull(compact);
    }

    @Test
    public void testOne_ToJson_ObjectToFile() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        Person person = new Person("Charlie", 35);
        File outputFile = new File(tempDir, "one-person.json");

        one.toJson(person, outputFile);

        Assertions.assertTrue(outputFile.exists());
    }

    @Test
    public void testOne_ToJson_ObjectToOutputStream() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        Person person = new Person("Diana", 28);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        one.toJson(person, baos);

        String json = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(json.contains("Diana"));
    }

    @Test
    public void testOne_ToJson_ObjectToWriter() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        Person person = new Person("Eve", 32);
        StringWriter writer = new StringWriter();

        one.toJson(person, writer);

        String json = writer.toString();
        Assertions.assertTrue(json.contains("Eve"));
    }

    @Test
    public void testOne_ToJson_ObjectToDataOutput() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        Person person = new Person("Frank", 40);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutput dos = new DataOutputStream(baos);

        one.toJson(person, dos);

        String json = baos.toString(StandardCharsets.UTF_8.name());
        Assertions.assertTrue(json.contains("Frank"));
    }

    @Test
    public void testOne_FromJson_ByteArray() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String jsonString = "{\"name\":\"Grace\",\"age\":29}";
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        Person person = one.fromJson(jsonBytes, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Grace", person.name);
    }

    @Test
    public void testOne_FromJson_ByteArrayWithOffset() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String prefix = "XX";
        String jsonString = "{\"name\":\"Henry\",\"age\":50}";
        byte[] buffer = (prefix + jsonString).getBytes(StandardCharsets.UTF_8);

        Person person = one.fromJson(buffer, prefix.length(), jsonString.length(), Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Henry", person.name);
    }

    @Test
    public void testOne_FromJson_String() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "{\"name\":\"Ivy\",\"age\":27}";

        Person person = one.fromJson(json, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Ivy", person.name);
    }

    @Test
    public void testOne_FromJson_File() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        File jsonFile = new File(tempDir, "one-test.json");
        String json = "{\"name\":\"Jack\",\"age\":33}";
        Files.writeString(jsonFile.toPath(), json);

        Person person = one.fromJson(jsonFile, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Jack", person.name);
    }

    @Test
    public void testOne_FromJson_InputStream() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "{\"name\":\"Kate\",\"age\":26}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        Person person = one.fromJson(bais, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Kate", person.name);
    }

    @Test
    public void testOne_FromJson_Reader() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "{\"name\":\"Leo\",\"age\":31}";
        StringReader reader = new StringReader(json);

        Person person = one.fromJson(reader, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Leo", person.name);
    }

    @Test
    public void testOne_FromJson_URL() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        File jsonFile = new File(tempDir, "one-url.json");
        String json = "{\"name\":\"Mary\",\"age\":24}";
        Files.writeString(jsonFile.toPath(), json);
        URL url = jsonFile.toURI().toURL();

        Person person = one.fromJson(url, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Mary", person.name);
    }

    @Test
    public void testOne_FromJson_DataInput() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "{\"name\":\"Nancy\",\"age\":22}";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DataInput dis = new DataInputStream(bais);

        Person person = one.fromJson(dis, Person.class);

        Assertions.assertNotNull(person);
        Assertions.assertEquals("Nancy", person.name);
    }

    @Test
    public void testOne_FromJson_ByteArrayWithTypeReference() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String jsonString = "[{\"name\":\"Oscar\",\"age\":38}]";
        byte[] jsonBytes = jsonString.getBytes(StandardCharsets.UTF_8);

        List<Person> people = one.fromJson(jsonBytes, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals(1, people.size());
        Assertions.assertEquals("Oscar", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_ByteArrayWithOffsetAndTypeReference() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String prefix = "YY";
        String jsonString = "[{\"name\":\"Paul\",\"age\":42}]";
        byte[] buffer = (prefix + jsonString).getBytes(StandardCharsets.UTF_8);

        List<Person> people = one.fromJson(buffer, prefix.length(), jsonString.length(), new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Paul", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_StringWithTypeReference() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "[{\"name\":\"Quinn\",\"age\":36}]";

        List<Person> people = one.fromJson(json, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Quinn", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_FileWithTypeReference() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        File jsonFile = new File(tempDir, "one-list.json");
        String json = "[{\"name\":\"Rachel\",\"age\":29}]";
        Files.writeString(jsonFile.toPath(), json);

        List<Person> people = one.fromJson(jsonFile, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Rachel", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_InputStreamWithTypeReference() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "[{\"name\":\"Sam\",\"age\":34}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

        List<Person> people = one.fromJson(bais, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Sam", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_ReaderWithTypeReference() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "[{\"name\":\"Tina\",\"age\":27}]";
        StringReader reader = new StringReader(json);

        List<Person> people = one.fromJson(reader, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Tina", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_URLWithTypeReference() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        File jsonFile = new File(tempDir, "one-url-list.json");
        String json = "[{\"name\":\"Uma\",\"age\":41}]";
        Files.writeString(jsonFile.toPath(), json);
        URL url = jsonFile.toURI().toURL();

        List<Person> people = one.fromJson(url, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Uma", people.get(0).name);
    }

    @Test
    public void testOne_FromJson_DataInputWithTypeReference() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonMappers.One one = JsonMappers.wrap(mapper);

        String json = "[{\"name\":\"Victor\",\"age\":39}]";
        ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        DataInput dis = new DataInputStream(bais);

        List<Person> people = one.fromJson(dis, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(people);
        Assertions.assertEquals("Victor", people.get(0).name);
    }

    @Test
    public void testRoundTrip() {
        Person original = new Person("RoundTrip", 99);
        String json = JsonMappers.toJson(original);
        Person deserialized = JsonMappers.fromJson(json, Person.class);

        Assertions.assertEquals(original, deserialized);
    }

    @Test
    public void testComplexTypeWithMap() {
        Map<String, Person> map = new HashMap<>();
        map.put("person1", new Person("Map1", 20));
        map.put("person2", new Person("Map2", 21));

        String json = JsonMappers.toJson(map);
        Map<String, Person> deserialized = JsonMappers.fromJson(json, new TypeReference<Map<String, Person>>() {
        });

        Assertions.assertEquals(2, deserialized.size());
        Assertions.assertEquals("Map1", deserialized.get("person1").name);
    }

    @Test
    public void testListSerialization() {
        List<Person> people = new ArrayList<>();
        people.add(new Person("List1", 25));
        people.add(new Person("List2", 26));

        String json = JsonMappers.toJson(people);
        List<Person> deserialized = JsonMappers.fromJson(json, new TypeReference<List<Person>>() {
        });

        Assertions.assertEquals(2, deserialized.size());
    }

    @Test
    public void testEmptyCollections() {
        List<Person> empty = new ArrayList<>();
        String json = JsonMappers.toJson(empty);
        List<Person> deserialized = JsonMappers.fromJson(json, new TypeReference<List<Person>>() {
        });

        Assertions.assertNotNull(deserialized);
        Assertions.assertEquals(0, deserialized.size());
    }

    @Test
    public void testPrimitiveTypes() {
        String intJson = JsonMappers.toJson(42);
        Integer intValue = JsonMappers.fromJson(intJson, Integer.class);
        Assertions.assertEquals(42, intValue);

        String stringJson = JsonMappers.toJson("test");
        String stringValue = JsonMappers.fromJson(stringJson, String.class);
        Assertions.assertEquals("test", stringValue);
    }
}
