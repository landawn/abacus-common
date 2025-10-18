package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class JSONHttpMessageConverter100Test extends TestBase {

    private JSONHttpMessageConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new JSONHttpMessageConverter();
    }

    @Test
    public void testConstructor() {
        JSONHttpMessageConverter newConverter = new JSONHttpMessageConverter();
        assertNotNull(newConverter);

        List<MediaType> supportedMediaTypes = newConverter.getSupportedMediaTypes();
        assertTrue(supportedMediaTypes.contains(MediaType.APPLICATION_JSON));
        assertTrue(supportedMediaTypes.contains(new MediaType("application", "*+json")));
    }

    @Test
    public void testReadInternal_SimpleObject() throws IOException {
        String json = "{\"name\":\"John\",\"age\":30,\"active\":true}";
        StringReader reader = new StringReader(json);

        Object result = converter.readInternal(TestPerson.class, reader);

        assertNotNull(result);
        assertTrue(result instanceof TestPerson);
        TestPerson person = (TestPerson) result;
        assertEquals("John", person.name);
        assertEquals(30, person.age);
        assertTrue(person.active);
    }

    @Test
    public void testReadInternal_List() throws IOException {
        String json = "[\"item1\",\"item2\",\"item3\"]";
        StringReader reader = new StringReader(json);

        Type listType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { String.class };
            }

            @Override
            public Type getRawType() {
                return List.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Object result = converter.readInternal(listType, reader);

        assertNotNull(result);
        assertTrue(result instanceof List);
        List<?> list = (List<?>) result;
        assertEquals(3, list.size());
        assertEquals("item1", list.get(0));
        assertEquals("item2", list.get(1));
        assertEquals("item3", list.get(2));
    }

    @Test
    public void testReadInternal_Map() throws IOException {
        String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
        StringReader reader = new StringReader(json);

        Type mapType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { String.class, String.class };
            }

            @Override
            public Type getRawType() {
                return Map.class;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Object result = converter.readInternal(mapType, reader);

        assertNotNull(result);
        assertTrue(result instanceof Map);
        Map<?, ?> map = (Map<?, ?>) result;
        assertEquals(2, map.size());
        assertEquals("value1", map.get("key1"));
        assertEquals("value2", map.get("key2"));
    }

    @Test
    public void testReadInternal_Array() throws IOException {
        String json = "[1,2,3,4,5]";
        StringReader reader = new StringReader(json);

        Object result = converter.readInternal(int[].class, reader);

        assertNotNull(result);
        assertTrue(result instanceof int[]);
        int[] array = (int[]) result;
        assertEquals(5, array.length);
        assertEquals(1, array[0]);
        assertEquals(5, array[4]);
    }

    @Test
    public void testReadInternal_NestedObject() throws IOException {
        String json = "{\"person\":{\"name\":\"Jane\",\"age\":25,\"active\":false},\"score\":95.5}";
        StringReader reader = new StringReader(json);

        Object result = converter.readInternal(TestWrapper.class, reader);

        assertNotNull(result);
        assertTrue(result instanceof TestWrapper);
        TestWrapper wrapper = (TestWrapper) result;
        assertNotNull(wrapper.person);
        assertEquals("Jane", wrapper.person.name);
        assertEquals(25, wrapper.person.age);
        assertFalse(wrapper.person.active);
        assertEquals(95.5, wrapper.score);
    }

    @Test
    public void testReadInternal_Null() throws IOException {
        String json = "";
        StringReader reader = new StringReader(json);

        Object result = converter.readInternal(TestPerson.class, reader);

        assertNull(result);
    }

    @Test
    public void testReadInternal_EmptyObject() throws IOException {
        String json = "{}";
        StringReader reader = new StringReader(json);

        Object result = converter.readInternal(TestPerson.class, reader);

        assertNotNull(result);
        assertTrue(result instanceof TestPerson);
        TestPerson person = (TestPerson) result;
        assertNull(person.name);
        assertEquals(0, person.age);
        assertFalse(person.active);
    }

    @Test
    public void testWriteInternal_SimpleObject() throws IOException {
        TestPerson person = new TestPerson();
        person.name = "Alice";
        person.age = 28;
        person.active = true;

        StringWriter writer = new StringWriter();
        converter.writeInternal(person, TestPerson.class, writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"Alice\""));
        assertTrue(json.contains("\"age\": 28"));
        assertTrue(json.contains("\"active\": true"));
    }

    @Test
    public void testWriteInternal_List() throws IOException {
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("banana");
        list.add("cherry");

        StringWriter writer = new StringWriter();
        converter.writeInternal(list, List.class, writer);

        String json = writer.toString();
        assertEquals("[\"apple\", \"banana\", \"cherry\"]", json);
    }

    @Test
    public void testWriteInternal_Map() throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "test");
        map.put("number", 42);
        map.put("boolean", true);

        StringWriter writer = new StringWriter();
        converter.writeInternal(map, Map.class, writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("\"string\": \"test\""));
        assertTrue(json.contains("\"number\": 42"));
        assertTrue(json.contains("\"boolean\": true"));
    }

    @Test
    public void testWriteInternal_Array() throws IOException {
        int[] array = { 10, 20, 30, 40, 50 };

        StringWriter writer = new StringWriter();
        converter.writeInternal(array, int[].class, writer);

        String json = writer.toString();
        assertEquals("[10, 20, 30, 40, 50]", json);
    }

    @Test
    public void testWriteInternal_NestedObject() throws IOException {
        TestWrapper wrapper = new TestWrapper();
        wrapper.person = new TestPerson();
        wrapper.person.name = "Bob";
        wrapper.person.age = 35;
        wrapper.person.active = false;
        wrapper.score = 88.8;

        StringWriter writer = new StringWriter();
        converter.writeInternal(wrapper, TestWrapper.class, writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("\"person\": {"));
        assertTrue(json.contains("\"name\": \"Bob\""));
        assertTrue(json.contains("\"age\": 35"));
        assertTrue(json.contains("\"active\": false"));
        assertTrue(json.contains("\"score\": 88.8"));
    }

    @Test
    public void testWriteInternal_Null() throws IOException {
        StringWriter writer = new StringWriter();
        converter.writeInternal(null, Object.class, writer);

        String json = writer.toString();
        assertEquals("", json);
    }

    @Test
    public void testWriteInternal_EmptyObject() throws IOException {
        TestPerson person = new TestPerson();

        StringWriter writer = new StringWriter();
        converter.writeInternal(person, TestPerson.class, writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("\"age\": 0"));
        assertTrue(json.contains("\"active\": false"));
    }

    @Test
    public void testWriteInternal_TypeParameterUnused() throws IOException {
        TestPerson person = new TestPerson();
        person.name = "Test";
        person.age = 40;

        StringWriter writer = new StringWriter();
        converter.writeInternal(person, String.class, writer);

        String json = writer.toString();
        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"Test\""));
        assertTrue(json.contains("\"age\": 40"));
    }

    @Test
    public void testCanRead() {
        assertTrue(converter.canRead(TestPerson.class, MediaType.APPLICATION_JSON));
        assertTrue(converter.canRead(List.class, MediaType.APPLICATION_JSON));
        assertTrue(converter.canRead(Map.class, MediaType.APPLICATION_JSON));
        assertTrue(converter.canRead(String.class, MediaType.APPLICATION_JSON));

        assertTrue(converter.canRead(TestPerson.class, new MediaType("application", "vnd.api+json")));

        assertFalse(converter.canRead(TestPerson.class, MediaType.APPLICATION_XML));
    }

    @Test
    public void testCanWrite() {
        assertTrue(converter.canWrite(TestPerson.class, MediaType.APPLICATION_JSON));
        assertTrue(converter.canWrite(List.class, MediaType.APPLICATION_JSON));
        assertTrue(converter.canWrite(Map.class, MediaType.APPLICATION_JSON));
        assertTrue(converter.canWrite(String.class, MediaType.APPLICATION_JSON));

        assertTrue(converter.canWrite(TestPerson.class, new MediaType("application", "vnd.api+json")));

        assertFalse(converter.canWrite(TestPerson.class, MediaType.APPLICATION_XML));
    }

    @Test
    public void testReadFromHttpInputMessage() throws IOException {
        String json = "{\"name\":\"Charlie\",\"age\":45,\"active\":true}";
        MockHttpInputMessage inputMessage = new MockHttpInputMessage(json);
        inputMessage.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        TestPerson person = (TestPerson) converter.read(TestPerson.class, inputMessage);

        assertNotNull(person);
        assertEquals("Charlie", person.name);
        assertEquals(45, person.age);
        assertTrue(person.active);
    }

    @Test
    public void testWriteToHttpOutputMessage() throws IOException {
        TestPerson person = new TestPerson();
        person.name = "David";
        person.age = 50;
        person.active = false;

        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(person, MediaType.APPLICATION_JSON, outputMessage);

        String json = outputMessage.getBodyAsString();
        assertNotNull(json);
        assertTrue(json.contains("\"name\": \"David\""));
        assertTrue(json.contains("\"age\": 50"));
        assertTrue(json.contains("\"active\": false"));
    }

    @Test
    public void testComplexGenericType() throws IOException {
        String json = "{\"items\":[{\"name\":\"Item1\",\"age\":10,\"active\":true}," + "{\"name\":\"Item2\",\"age\":20,\"active\":false}],\"count\":2}";
        StringReader reader = new StringReader(json);

        Object result = converter.readInternal(TestGenericWrapper.class, reader);

        assertNotNull(result);
        assertTrue(result instanceof TestGenericWrapper);
        TestGenericWrapper wrapper = (TestGenericWrapper) result;
        assertEquals(2, wrapper.count);
        assertNotNull(wrapper.items);
        assertEquals(2, wrapper.items.size());
        assertEquals("Item1", wrapper.items.get(0).name);
        assertEquals("Item2", wrapper.items.get(1).name);
    }

    public static class TestPerson {
        public String name;
        public int age;
        public boolean active;
    }

    public static class TestWrapper {
        public TestPerson person;
        public double score;
    }

    public static class TestGenericWrapper {
        public List<TestPerson> items;
        public int count;
    }

    private static class MockHttpInputMessage implements HttpInputMessage {
        private final byte[] body;
        private final org.springframework.http.HttpHeaders headers;

        MockHttpInputMessage(String content) {
            this.body = content.getBytes(StandardCharsets.UTF_8);
            this.headers = new org.springframework.http.HttpHeaders();
        }

        @Override
        public java.io.InputStream getBody() throws IOException {
            return new ByteArrayInputStream(body);
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return headers;
        }
    }

    private static class MockHttpOutputMessage implements HttpOutputMessage {
        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private final org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();

        @Override
        public java.io.OutputStream getBody() throws IOException {
            return outputStream;
        }

        @Override
        public org.springframework.http.HttpHeaders getHeaders() {
            return headers;
        }

        String getBodyAsString() {
            return outputStream.toString(StandardCharsets.UTF_8);
        }
    }
}
