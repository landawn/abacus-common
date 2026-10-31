package com.landawn.abacus.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;

public class JsonHttpMessageConverterTest extends TestBase {

    private JsonHttpMessageConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new JsonHttpMessageConverter();
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

    @Test
    public void testConstructor() {
        JsonHttpMessageConverter newConverter = new JsonHttpMessageConverter();
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
        assertEquals("null", json);
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

    // ========== M55: media-type constructor overloads ==========

    @Test
    public void testConstructor_MediaTypeVarargs() {
        MediaType vendor = new MediaType("application", "vnd.api+json");
        JsonHttpMessageConverter c = new JsonHttpMessageConverter(MediaType.APPLICATION_JSON, vendor);

        List<MediaType> supported = c.getSupportedMediaTypes();
        assertEquals(2, supported.size());
        assertTrue(supported.contains(MediaType.APPLICATION_JSON));
        assertTrue(supported.contains(vendor));
        // The custom media types are actually honored by content negotiation.
        assertTrue(c.canRead(TestPerson.class, vendor));
        assertTrue(c.canWrite(TestPerson.class, vendor));
        assertFalse(c.canRead(TestPerson.class, MediaType.TEXT_PLAIN));
    }

    @Test
    public void testConstructor_MediaTypeVarargs_EmptyKeepsDefaults() {
        JsonHttpMessageConverter c = new JsonHttpMessageConverter(new MediaType[0]);

        // With no media types supplied, the parent defaults are retained.
        List<MediaType> supported = c.getSupportedMediaTypes();
        assertTrue(supported.contains(MediaType.APPLICATION_JSON));
    }

    @Test
    public void testConstructor_Config_MediaTypeVarargs() {
        MediaType textJson = new MediaType("text", "json");
        JsonHttpMessageConverter c = new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), textJson);

        List<MediaType> supported = c.getSupportedMediaTypes();
        assertEquals(1, supported.size());
        assertTrue(supported.contains(textJson));
        assertTrue(c.canWrite(TestPerson.class, textJson));
    }

    @Test
    public void testConstructor_Config_MediaTypeList() {
        MediaType vendor = new MediaType("application", "vnd.api+json");
        JsonHttpMessageConverter c = new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), List.of(MediaType.APPLICATION_JSON, vendor));

        List<MediaType> supported = c.getSupportedMediaTypes();
        assertEquals(2, supported.size());
        assertTrue(supported.contains(vendor));
    }

    @Test
    public void testConstructor_Config_MediaTypeList_NullKeepsDefaults() {
        JsonHttpMessageConverter c = new JsonHttpMessageConverter(new JsonSerConfig(), new JsonDeserConfig(), (List<MediaType>) null);

        assertTrue(c.getSupportedMediaTypes().contains(MediaType.APPLICATION_JSON));
    }

    // a14 F-3: type mismatches surface through Spring's read(...) as HttpMessageNotReadableException; the cause is
    // whatever the abacus parser threw (never IllegalArgumentException), and an object body read into a List<T>
    // is lenient. These tests pin the documented contract.
    private static MockHttpInputMessage jsonMessage(String json) {
        MockHttpInputMessage message = new MockHttpInputMessage(json);
        message.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return message;
    }

    @Test
    public void testRead_scalarTypeMismatch_wrappedWithNumberFormatExceptionCause() {
        HttpMessageNotReadableException e = assertThrows(HttpMessageNotReadableException.class,
                () -> converter.read(Integer.class, null, jsonMessage("\"abc\"")));
        // Exactly NumberFormatException (an IllegalArgumentException subclass), not a bare IllegalArgumentException.
        assertEquals(NumberFormatException.class, e.getCause().getClass(), String.valueOf(e.getCause()));
    }

    @Test
    public void testRead_arrayBodyForBeanTarget_wrappedWithParsingExceptionCause() {
        HttpMessageNotReadableException e = assertThrows(HttpMessageNotReadableException.class,
                () -> converter.read(TestPerson.class, null, jsonMessage("[1,2]")));
        assertInstanceOf(ParsingException.class, e.getCause(), String.valueOf(e.getCause()));
    }

    @Test
    public void testRead_propertyTypeMismatch_wrappedWithNumberFormatExceptionCause() {
        HttpMessageNotReadableException e = assertThrows(HttpMessageNotReadableException.class,
                () -> converter.read(TestPerson.class, null, jsonMessage("{\"age\":\"x\"}")));
        assertInstanceOf(NumberFormatException.class, e.getCause(), String.valueOf(e.getCause()));
        // The plain Class overload takes the same path.
        assertThrows(HttpMessageNotReadableException.class, () -> converter.read(TestPerson.class, jsonMessage("{\"age\":\"λ\"}")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testRead_objectBodyIntoListTarget_isLenientOneElementMap() throws IOException {
        Type listOfString = new ParameterizedType() {
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

        Object result = converter.read(listOfString, null, jsonMessage("{\"a\":1}"));

        // An object body is still accepted for a List target and still yields exactly one element. The element type
        // declared by the target is now honoured: it used to be read as a Map whatever the declaration said (the same
        // defect gave a HashMap[] for a String[] target), so a List<String> now receives the value's JSON text.
        assertInstanceOf(List.class, result);
        List<Object> list = (List<Object>) result;
        assertEquals(1, list.size());
        assertInstanceOf(String.class, list.get(0), String.valueOf(list.get(0)));
        assertEquals("{\"a\": 1}", list.get(0));

        // ... and an Object element type still receives the Map, which is what an untyped body means.
        Type listOfObject = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { Object.class };
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

        List<Object> untyped = (List<Object>) converter.read(listOfObject, null, jsonMessage("{\"a\":1}"));
        assertEquals(1, untyped.size());
        assertInstanceOf(Map.class, untyped.get(0), String.valueOf(untyped.get(0)));
        assertEquals(1, ((Map<String, Object>) untyped.get(0)).get("a"));

        // Regression guard: a genuine array body still yields the declared element type.
        List<Object> proper = (List<Object>) converter.read(listOfString, null, jsonMessage("[\"x\",\"λ\"]"));
        assertEquals(List.of("x", "λ"), proper);
    }

    private static Type parameterized(final Type raw, final Type... args) {
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return args;
            }

            @Override
            public Type getRawType() {
                return raw;
            }

            @Override
            public Type getOwnerType() {
                return null;
            }
        };
    }

    /**
     * {@code Multiset} and {@code Multimap} satisfy {@code isStructuredValueType}'s stated rule exactly -
     * they report neither isArray nor isCollection nor isMap, they are serializable, and their {@code stringOf}
     * emits JSON object text - but they were missing from its list, so the writer fell through to the scalar
     * path and emitted the container's JSON as a quoted, escaped <i>string</i>, while the reader rejected a
     * well-formed object body outright.
     */
    @Test
    public void test_multisetAndMultimap_areStructuredNotScalar_regression_20260918() throws IOException {
        final Multiset<String> multiset = N.newMultiset();
        multiset.add("x", 2);

        final Type multisetType = parameterized(Multiset.class, String.class);
        StringWriter writer = new StringWriter();
        converter.writeInternal(multiset, multisetType, writer);
        // Before the fix this was the quoted string "{\"x\": 2}" instead of a JSON object.
        assertEquals("{\"x\": 2}", writer.toString());

        final ListMultimap<String, String> multimap = N.newListMultimap();
        multimap.put("k", "v");

        final Type multimapType = parameterized(ListMultimap.class, String.class, String.class);
        writer = new StringWriter();
        converter.writeInternal(multimap, multimapType, writer);
        assertEquals("{\"k\": [\"v\"]}", writer.toString());

        // Before the fix both of these threw "Expected one JSON scalar value".
        final Object readMultiset = converter.read(multisetType, null, jsonMessage("{\"x\":2}"));
        assertInstanceOf(Multiset.class, readMultiset);
        assertEquals(2, ((Multiset<?>) readMultiset).count("x"));

        final Object readMultimap = converter.read(multimapType, null, jsonMessage("{\"k\":[\"v\"]}"));
        assertInstanceOf(ListMultimap.class, readMultimap);
        assertEquals(List.of("v"), ((ListMultimap<?, ?>) readMultimap).get("k"));
    }

    // ---- deep review 2026-10-27 G138 begin ----
    public static class FormattedNumberBean {
        @com.landawn.abacus.annotation.JsonXmlField(numberFormat = "#.##")
        public double d;
        public String s = "x";
    }

    public static class TransientNumberBean {
        public transient double t;
        public String s = "x";
    }

    private static List<Object> containersOf(final double d) {
        final TestWrapper wrapper = new TestWrapper();
        wrapper.score = d;
        final List<Object> values = new ArrayList<>();
        values.add(List.of(d));
        values.add(Map.of("a", d));
        values.add(wrapper);
        values.add(new double[] { 1, d });
        values.add(new Double[] { d });
        values.add(new Object[] { "s", d });
        values.add(new float[] { (float) d });
        values.add(new Float[] { (float) d });
        values.add(com.landawn.abacus.util.u.Optional.of(Map.of("a", d)));
        values.add(com.landawn.abacus.util.u.Nullable.of(List.of(d)));
        values.add(java.util.Optional.of(new double[] { d }));
        values.add(List.of(com.landawn.abacus.util.u.Optional.of(d)));
        values.add(List.of(com.landawn.abacus.util.u.Nullable.of(d)));
        values.add(List.of(java.util.OptionalDouble.of(d)));
        values.add(List.of(List.of(Map.of("deep", d))));
        values.add(com.landawn.abacus.util.Pair.of(1.0, d));
        values.add(com.landawn.abacus.util.DoubleList.of(d));
        return values;
    }

    // r108 wrote a nonfinite double/float nested in a non-scalar root as a bare NaN/Infinity/-Infinity token ([NaN],
    // {"a": Infinity}, {"score": -Infinity, ...}), which is not JSON; only a scalar root was rejected.
    @Test
    public void testWriteInternal_nestedNonfiniteNumbersAreRejected() {
        for (final double d : new double[] { Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            final String token = Double.isNaN(d) ? "NaN" : (d > 0 ? "Infinity" : "-Infinity");
            for (final Object value : containersOf(d)) {
                final StringWriter writer = new StringWriter();
                final ParsingException e = assertThrows(ParsingException.class, () -> converter.writeInternal(value, value.getClass(), writer),
                        () -> token + " in " + value.getClass().getName() + " was written as " + writer);
                assertEquals("Serialization did not produce a valid JSON value: the nonfinite number " + token + " has no JSON representation", e.getMessage());
                // A small response fails before anything reaches the writer, like a nonfinite scalar root.
                assertEquals("", writer.toString(), value.getClass().getName());
            }
        }

        // The same through Spring's write(..): HttpMessageNotWritableException with the ParsingException as its cause, empty body.
        final MockHttpOutputMessage message = new MockHttpOutputMessage();
        final Exception e = assertThrows(org.springframework.http.converter.HttpMessageNotWritableException.class,
                () -> converter.write(Map.of("a", Float.NaN), MediaType.APPLICATION_JSON, message));
        assertInstanceOf(ParsingException.class, e.getCause());
        assertEquals("", message.getBodyAsString());

        // Pretty format, a wrapped root and a large response are checked too.
        final JsonHttpMessageConverter pretty = new JsonHttpMessageConverter(new JsonSerConfig().setPrettyFormat(true).setWrapRootValue(true),
                new JsonDeserConfig());
        assertThrows(ParsingException.class, () -> pretty.writeInternal(containersOf(Double.NaN).get(2), TestWrapper.class, new StringWriter()));
        final List<Object> large = new ArrayList<>();
        for (int i = 0; i < 20_000; i++) {
            large.add(1.0 / 3);
        }
        large.add(Double.NEGATIVE_INFINITY);
        assertThrows(ParsingException.class, () -> converter.writeInternal(large, List.class, new StringWriter()));
    }

    // Finite numbers, and nonfinite values that the serializer writes as JSON strings or does not write at all, keep their r108 output.
    @Test
    public void testWriteInternal_finiteAndStringWrittenNumbersAreUnchanged() {
        final java.util.function.Function<Object, String> write = value -> {
            final StringWriter writer = new StringWriter();
            converter.writeInternal(value, value.getClass(), writer);
            return writer.toString();
        };

        assertEquals("[1.5, -0.0, 1.0E300, 4.9E-324, 1.0E-10]", write.apply(List.of(1.5, -0.0, 1e300, Double.MIN_VALUE, 1e-10)));
        assertEquals("[\"NaN\", \"Infinity\", \"-Infinity\", \"a\\\"NaN\", \"\\\\\\\"Infinity\"]",
                write.apply(List.of("NaN", "Infinity", "-Infinity", "a\"NaN", "\\\"Infinity")));
        assertEquals("[\"N\", \"I\"]", write.apply(List.of('N', 'I')));
        assertEquals("{\"NaN\": \"v\"}", write.apply(Map.of(Double.NaN, "v")));
        assertEquals("{\"-Infinity\": \"Infinity\"}", write.apply(Map.of(Double.NEGATIVE_INFINITY, "Infinity")));
        assertEquals("[1.0, -0.0]", write.apply(com.landawn.abacus.util.Pair.of(1.0, -0.0)));

        final FormattedNumberBean formatted = new FormattedNumberBean();
        formatted.d = Double.NaN;
        assertEquals("{\"d\": \"NaN\", \"s\": \"x\"}", write.apply(formatted));

        final TransientNumberBean transientBean = new TransientNumberBean();
        transientBean.t = Double.POSITIVE_INFINITY;
        assertEquals("{\"s\": \"x\"}", write.apply(transientBean));

        final JsonSerConfig ignoring = new JsonSerConfig();
        ignoring.setIgnoredPropNames(TestWrapper.class, N.asSet("score"));
        final TestWrapper wrapper = new TestWrapper();
        wrapper.score = Double.NaN;
        final StringWriter writer = new StringWriter();
        new JsonHttpMessageConverter(ignoring, new JsonDeserConfig()).writeInternal(wrapper, TestWrapper.class, writer);
        assertEquals("{}", writer.toString());

        // A nonfinite scalar root keeps its own message.
        assertEquals("Scalar serialization did not produce a valid JSON value",
                assertThrows(ParsingException.class, () -> converter.writeInternal(Double.NaN, Double.class, new StringWriter())).getMessage());
    }
    // ---- deep review 2026-10-27 G138 end ----

    // ---- deep review 2026-10-27 verify G146 begin ----
    private static final String NONFINITE_MESSAGE = "Serialization did not produce a valid JSON value: the nonfinite number %s has no JSON representation";

    // Records what reaches the caller's writer, including flush() and close().
    private static final class RecordingWriter extends java.io.Writer {
        private final StringBuilder text = new StringBuilder();
        private int flushes;
        private int closes;

        @Override
        public void write(final char[] cbuf, final int off, final int len) {
            text.append(cbuf, off, len);
        }

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closes++;
        }
    }

    // Fails every write, as a servlet output stream does after the client went away.
    private static final class FailingWriter extends java.io.Writer {
        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            throw new IOException("Broken pipe");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static java.io.Writer newNonfiniteNumberGuard(final java.io.Writer target) throws ReflectiveOperationException {
        final java.lang.reflect.Constructor<?> constructor = Class.forName(JsonHttpMessageConverter.class.getName() + "$NonfiniteNumberGuard")
                .getDeclaredConstructor(java.io.Writer.class);
        constructor.setAccessible(true);
        return (java.io.Writer) constructor.newInstance(target);
    }

    private static void write(final java.io.Writer writer, final String[] chunks, final int mode) throws IOException {
        for (final String chunk : chunks) {
            if (mode == 0) {
                writer.write(chunk.toCharArray(), 0, chunk.length());
            } else if (mode == 1) {
                writer.write(chunk, 0, chunk.length());
            } else {
                for (int i = 0; i < chunk.length(); i++) {
                    writer.write(chunk.charAt(i));
                }
            }
        }
    }

    // A target that fails to take a chunk surfaces its own IOException, as in r108. The serializer re-sends the failed chunk while
    // it unwinds; scanning it a second time, from the state after it, read string content as bare text and reported a
    // nonexistent "nonfinite number NaN" for strings like "NaN" (the padding covers every chunk alignment).
    @Test
    public void testWriteInternal_failingTargetReportsItsOwnFailure() {
        for (final String word : new String[] { "NaN", "Infinity", "x" }) {
            for (int pad = 0; pad < 12; pad++) {
                final List<Object> values = new ArrayList<>();
                values.add("p".repeat(pad));
                for (int i = 0; i < 4000; i++) {
                    values.add(word);
                }

                final Exception e = assertThrows(com.landawn.abacus.exception.UncheckedIOException.class,
                        () -> converter.writeInternal(values, List.class, new FailingWriter()), word + " pad=" + pad);
                assertInstanceOf(IOException.class, e.getCause(), word + " pad=" + pad);
                assertEquals("Broken pipe", e.getCause().getMessage());
            }
        }
    }

    // A null writer is an IllegalArgumentException for a bean, map, collection or array root, as in r108.
    @Test
    public void testWriteInternal_nullWriterIsIllegalArgument() {
        for (final Object value : new Object[] { List.of(1.5), Map.of("a", 1), new TestPerson(), new double[] { 1 } }) {
            assertThrows(IllegalArgumentException.class, () -> converter.writeInternal(value, value.getClass(), null), value.getClass().getName());
        }
    }

    // The caller's writer is flushed once and never closed, as in r108.
    @Test
    public void testWriteInternal_targetIsFlushedOnceAndNeverClosed() {
        final RecordingWriter target = new RecordingWriter();
        converter.writeInternal(List.of("NaN", 1.5, Map.of("Infinity", "-Infinity")), List.class, target);
        assertEquals("[\"NaN\", 1.5, {\"Infinity\": \"-Infinity\"}]", target.text.toString());
        assertEquals(1, target.flushes);
        assertEquals(0, target.closes);
    }

    // A rejected value reaches the caller's writer not at all, not even as a flush, and the converter stays usable.
    @Test
    public void testWriteInternal_rejectedValueReachesTargetNotAtAll() {
        final RecordingWriter rejected = new RecordingWriter();
        assertThrows(ParsingException.class, () -> converter.writeInternal(List.of("x", Double.NaN), List.class, rejected));
        assertEquals("", rejected.text.toString());
        assertEquals(0, rejected.flushes);
        assertEquals(0, rejected.closes);

        final StringWriter again = new StringWriter();
        converter.writeInternal(List.of(2.5), List.class, again);
        assertEquals("[2.5]", again.toString());
    }

    // Strings longer than the serializer's buffer, full of quotes, backslashes and the words NaN/Infinity, reach the writer in several
    // chunks: their content never trips the check, a nonfinite number after them still does, and the finite output is unchanged.
    @Test
    public void testWriteInternal_longEscapedStringsAcrossChunks() {
        final String tricky = "\\\"NaN\\Infinity\"-Infinity \\u0022 ".repeat(6000);
        final List<Object> finite = List.of(tricky, 1.5, Map.of("NaN", tricky), 1e-5);
        final StringWriter writer = new StringWriter();
        converter.writeInternal(finite, List.class, writer);
        assertEquals(N.toJson(finite, new JsonSerConfig()), writer.toString());

        final ParsingException e = assertThrows(ParsingException.class,
                () -> converter.writeInternal(List.of(tricky, tricky, Double.NaN), List.class, new StringWriter()));
        assertEquals(String.format(NONFINITE_MESSAGE, "NaN"), e.getMessage());

        final StringWriter first = new StringWriter();
        assertEquals(String.format(NONFINITE_MESSAGE, "-Infinity"),
                assertThrows(ParsingException.class, () -> converter.writeInternal(List.of(Float.NEGATIVE_INFINITY, tricky), List.class, first)).getMessage());
        assertEquals("", first.toString());
    }

    // The check keeps its state between writes: an escape, a string or a token split across two chunks is read correctly, through
    // char[], String and single-char writes alike; after a failure nothing more is passed on, and close() never closes the target.
    @Test
    public void testNonfiniteNumberGuard_stateCarriesAcrossWrites() throws Exception {
        final String[][] rejected = { { "[\"a\\", "\"\", NaN]" }, { "[\"a\\", "\\\", NaN]" }, { "[Na", "N]" }, { "[-", "Infinity]" }, { "[Infin", "ity]" },
                { "{\"k\": [1e-5, ", "-Infinity]}" }, { "[\"\\\\\\\"\", ", "NaN]" } };
        final String[][] accepted = { { "[\"a\\", "\\", "\"", ", 1]" }, { "[\"NaN", "\"]" }, { "[\"\\", "u0022NaN\", 2]" },
                { "[1e", "-5, 1E", "+10, true, false, null]" }, { "{\"Infinity\": \"NaN\"", ", \"x\": \"-Infinity\"}" }, { "[Nan, NAN, infinity, Inf, Na]" } };

        for (int mode = 0; mode < 3; mode++) {
            final int currentMode = mode;

            for (final String[] chunks : rejected) {
                final String text = String.join("", chunks);
                final String word = text.contains("NaN") ? "NaN" : "Infinity";
                final String token = text.contains("-Infinity") ? "-Infinity" : word;
                final RecordingWriter target = new RecordingWriter();
                final java.io.Writer guard = newNonfiniteNumberGuard(target);
                final ParsingException e = assertThrows(ParsingException.class, () -> write(guard, chunks, currentMode), text + " mode " + currentMode);
                assertEquals(String.format(NONFINITE_MESSAGE, token), e.getMessage());

                guard.write("more");
                guard.write('x');
                guard.flush();
                guard.close();
                // The chunk (or char) completing the token is not passed on, nor anything after it.
                final String expected = mode < 2 ? String.join("", java.util.Arrays.copyOf(chunks, chunks.length - 1))
                        : text.substring(0, text.lastIndexOf(word) + word.length() - 1);
                assertEquals(expected, target.text.toString(), text + " mode " + mode);
                assertEquals(0, target.flushes);
                assertEquals(0, target.closes);
            }

            for (final String[] chunks : accepted) {
                final RecordingWriter target = new RecordingWriter();
                final java.io.Writer guard = newNonfiniteNumberGuard(target);
                write(guard, chunks, mode);
                guard.close();
                assertEquals(String.join("", chunks), target.text.toString(), "mode " + mode);
                assertEquals(1, target.flushes);
                assertEquals(0, target.closes);
            }
        }
    }
    // ---- deep review 2026-10-27 verify G146 end ----
}
