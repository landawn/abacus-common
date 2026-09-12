package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyChar;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonParser;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.BufferedXmlWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Optional;

public class ObjectArrayTypeTest extends TestBase {

    private ObjectArrayType<String> stringArrayType;
    private ObjectArrayType<Integer> intArrayType;
    private ObjectArrayType<Object> objectArrayType;
    private CharacterWriter writer;
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        stringArrayType = (ObjectArrayType<String>) createType(String[].class);
        intArrayType = (ObjectArrayType<Integer>) createType(Integer[].class);
        objectArrayType = (ObjectArrayType<Object>) createType(Object[].class);
        writer = createCharacterWriter();
        config = mock(JsonXmlSerConfig.class);
    }

    @Test
    public void testClazz() {
        assertEquals(String[].class, stringArrayType.javaType());
        assertEquals(Integer[].class, intArrayType.javaType());
        assertEquals(Object[].class, objectArrayType.javaType());
    }

    @Test
    public void testGetElementType() {
        assertEquals("String", stringArrayType.elementType().name());
        assertEquals("Integer", intArrayType.elementType().name());
        assertEquals("Object", objectArrayType.elementType().name());
    }

    @Test
    public void testIsObjectArray() {
        assertTrue(stringArrayType.isObjectArray());
        assertTrue(intArrayType.isObjectArray());
        assertTrue(objectArrayType.isObjectArray());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(stringArrayType.isSerializable());
        assertTrue(intArrayType.isSerializable());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(stringArrayType.stringOf(null));
    }

    @Test
    public void testStringOfWithEmptyArray() {
        String[] emptyArray = new String[0];
        assertEquals("[]", stringArrayType.stringOf(emptyArray));
    }

    @Test
    public void testStringOfWithSingleElement() {
        String[] array = { "test" };
        String result = stringArrayType.stringOf(array);
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void testStringOfWithMultipleElements() {
        String[] array = { "first", "second", "third" };
        String result = stringArrayType.stringOf(array);
        assertNotNull(result);
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
        assertTrue(result.contains("third"));
    }

    @Test
    public void testStringOfWithNullElements() {
        String[] array = { "first", null, "third" };
        String result = stringArrayType.stringOf(array);
        assertNotNull(result);
        assertTrue(result.contains("first"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("third"));
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(stringArrayType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        String[] result = stringArrayType.valueOf("");
        assertNull(result);
    }

    @Test
    public void testValueOfWithEmptyArrayString() {
        String[] result = stringArrayType.valueOf("[]");
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testValueOfWithValidJsonArray() {
        String[] result = stringArrayType.valueOf("[\"first\",\"second\",\"third\"]");
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals("first", result[0]);
        assertEquals("second", result[1]);
        assertEquals("third", result[2]);
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringArrayType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendToWithEmptyArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        String[] emptyArray = new String[0];
        stringArrayType.appendTo(sb, emptyArray);
        assertEquals("[]", sb.toString());
    }

    @Test
    public void testAppendToWithArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        String[] array = { "first", "second" };
        stringArrayType.appendTo(sb, array);
        String result = sb.toString();
        assertTrue(result.startsWith("["));
        assertTrue(result.endsWith("]"));
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
    }

    @Test
    public void testAppendToWithWriter() throws IOException {
        StringWriter stringWriter = new StringWriter();
        String[] array = { "test1", "test2" };
        stringArrayType.appendTo(stringWriter, array);
        String result = stringWriter.toString();
        assertTrue(result.contains("test1"));
        assertTrue(result.contains("test2"));
    }

    @Test
    public void testAppendToPropagatesWriterIOException() {
        final IOException failure = new IOException("write failure");
        final Writer writer = new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw failure;
            }

            @Override
            public void flush() {
                // no-op
            }

            @Override
            public void close() {
                // no-op
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> stringArrayType.appendTo(writer, new String[] { "test" })));
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        stringArrayType.serializeTo(writer, null, config);
        verify(writer).write(any(char[].class));
    }

    @Test
    public void testSerializeToWithEmptyArray() throws IOException {
        String[] emptyArray = new String[0];
        stringArrayType.serializeTo(writer, emptyArray, config);
        verify(writer, times(2)).write(anyChar());
    }

    @Test
    public void testSerializeToWithArray() throws IOException {
        String[] array = { "test" };
        stringArrayType.serializeTo(writer, array, config);
        verify(writer, atLeastOnce()).write(anyChar());
    }

    @Test
    public void testSerializeTo_NullStringElementHonorsWriteNullStringAsEmpty() throws IOException {
        com.landawn.abacus.util.BufferedJsonWriter actualWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            stringArrayType.serializeTo(actualWriter, new String[] { null }, com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullStringAsEmpty(true));
            assertEquals("[\"\"]", actualWriter.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(actualWriter);
        }
    }

    @Test
    public void testCollection2ArrayWithNull() {
        assertNull(stringArrayType.collectionToArray(null));
    }

    @Test
    public void testCollection2ArrayWithEmptyCollection() {
        List<String> emptyList = new ArrayList<>();
        String[] result = stringArrayType.collectionToArray(emptyList);
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void testCollection2ArrayWithElements() {
        List<String> list = Arrays.asList("one", "two", "three");
        String[] result = stringArrayType.collectionToArray(list);
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals("one", result[0]);
        assertEquals("two", result[1]);
        assertEquals("three", result[2]);
    }

    @Test
    public void testArray2CollectionWithArray() {
        List<String> list = new ArrayList<>();
        String[] array = { "a", "b", "c" };
        stringArrayType.arrayToCollection(array, list);
        assertEquals(3, list.size());
        assertTrue(list.contains("a"));
        assertTrue(list.contains("b"));
        assertTrue(list.contains("c"));
    }

    @Test
    public void testArray2CollectionWithNull() {
        List<String> list = new ArrayList<>();
        stringArrayType.arrayToCollection(null, list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testArray2CollectionWithEmptyArray() {
        List<String> list = new ArrayList<>();
        String[] emptyArray = new String[0];
        stringArrayType.arrayToCollection(emptyArray, list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testHashCode() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "a", "b", "c" };
        String[] array3 = { "x", "y", "z" };

        assertEquals(stringArrayType.hashCode(array1), stringArrayType.hashCode(array2));
        assertNotEquals(stringArrayType.hashCode(array1), stringArrayType.hashCode(array3));
    }

    @Test
    public void testDeepHashCode() {
        Object[] array1 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array2 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array3 = { new String[] { "x", "y" }, new String[] { "z", "w" } };

        assertEquals(objectArrayType.deepHashCode(array1), objectArrayType.deepHashCode(array2));
        assertNotEquals(objectArrayType.deepHashCode(array1), objectArrayType.deepHashCode(array3));
    }

    @Test
    public void testEquals() {
        String[] array1 = { "a", "b", "c" };
        String[] array2 = { "a", "b", "c" };
        String[] array3 = { "x", "y", "z" };

        assertTrue(stringArrayType.equals(array1, array2));
        assertFalse(stringArrayType.equals(array1, array3));
        assertTrue(stringArrayType.equals(null, null));
        assertFalse(stringArrayType.equals(array1, null));
        assertFalse(stringArrayType.equals(null, array1));
    }

    @Test
    public void testDeepEquals() {
        Object[] array1 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array2 = { new String[] { "a", "b" }, new String[] { "c", "d" } };
        Object[] array3 = { new String[] { "x", "y" }, new String[] { "z", "w" } };

        assertTrue(objectArrayType.deepEquals(array1, array2));
        assertFalse(objectArrayType.deepEquals(array1, array3));
        assertTrue(objectArrayType.deepEquals(null, null));
        assertFalse(objectArrayType.deepEquals(array1, null));
        assertFalse(objectArrayType.deepEquals(null, array1));
    }

    @Test
    public void testToString() {
        assertNull(stringArrayType.toString(null));
        assertEquals("[]", stringArrayType.toString(new String[0]));

        String[] array = { "hello", "world" };
        String result = stringArrayType.toString(array);
        assertNotNull(result);
        assertTrue(result.contains("hello"));
        assertTrue(result.contains("world"));
    }

    @Test
    public void testDeepToString() {
        assertNull(objectArrayType.deepToString(null));
        assertEquals("[]", objectArrayType.deepToString(new Object[0]));

        Object[] array = { new String[] { "a", "b" }, new Integer[] { 1, 2 } };
        String result = objectArrayType.deepToString(array);
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("1"));
        assertTrue(result.contains("2"));
    }

    // ---------------------------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06: T7-01 (serializeTo of non-serializable element types), T7-02 (no runtime-class
    // array from the parser), T7-07 (Unicode structural whitespace around the array).
    // ---------------------------------------------------------------------------------------------------------------

    public static class ReviewBean {
        private String name;
        private int age;

        public ReviewBean() {
        }

        public ReviewBean(final String name, final int age) {
            this.name = name;
            this.age = age;
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
    }

    public static class ReviewHolder {
        private Optional<Object[]> optObj;
        private Optional<ReviewBean[]> optBean;
        private Tuple2<Object[], String> tup;
        private Pair<Object[], String> pair;
        private Object[] plain;

        public Optional<Object[]> getOptObj() {
            return optObj;
        }

        public void setOptObj(final Optional<Object[]> optObj) {
            this.optObj = optObj;
        }

        public Optional<ReviewBean[]> getOptBean() {
            return optBean;
        }

        public void setOptBean(final Optional<ReviewBean[]> optBean) {
            this.optBean = optBean;
        }

        public Tuple2<Object[], String> getTup() {
            return tup;
        }

        public void setTup(final Tuple2<Object[], String> tup) {
            this.tup = tup;
        }

        public Pair<Object[], String> getPair() {
            return pair;
        }

        public void setPair(final Pair<Object[], String> pair) {
            this.pair = pair;
        }

        public Object[] getPlain() {
            return plain;
        }

        public void setPlain(final Object[] plain) {
            this.plain = plain;
        }
    }

    private static String serializeToJsonWriter(final Type<?> type, final Object value, final JsonXmlSerConfig<?> cfg) throws IOException {
        final BufferedJsonWriter w = Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(w, value, cfg);
            return w.toString();
        } finally {
            Objectory.recycle(w);
        }
    }

    private static String serializeToXmlWriter(final Type<?> type, final Object value, final JsonXmlSerConfig<?> cfg) throws IOException {
        final BufferedXmlWriter w = Objectory.createBufferedXmlWriter();

        try {
            ((Type<Object>) type).serializeTo(w, value, cfg);
            return w.toString();
        } finally {
            Objectory.recycle(w);
        }
    }

    private static void assertHeterogeneousElements(final Object[] a, final String label) {
        assertEquals(6, a.length, label);
        assertEquals(Integer.valueOf(1), a[0], label);
        assertEquals("a", a[1], label);
        assertNull(a[2], label);
        assertEquals(Double.valueOf(2.5), a[3], label);
        assertEquals(Boolean.TRUE, a[4], label);
        assertTrue(a[5] instanceof Map, label + ": " + a[5].getClass());
        assertEquals("z", ((Map<?, ?>) a[5]).get("name"), label);
        assertEquals(3, ((Number) ((Map<?, ?>) a[5]).get("age")).intValue(), label);
    }

    @Test
    public void reviewFixes20260906_serializeToWithJsonConfigWritesRealJsonForNonSerializableElementTypes() throws IOException {
        final Object[] het = { 1, "a", null, 2.5, true, new ReviewBean("z", 3) };
        final JsonSerConfig jsc = JsonSerConfig.create();

        // Before the fix every element went through SingleValueType.serializeTo: ["1", "a", null, "2.5", "true", "{\"name\"...}"]
        final String expected = "[1, \"a\", null, 2.5, true, {\"name\": \"z\", \"age\": 3}]";
        assertEquals(expected, serializeToJsonWriter(objectArrayType, het, jsc));
        assertEquals(Utils.jsonParser.serialize(het, jsc), serializeToJsonWriter(objectArrayType, het, jsc));
        assertEquals(objectArrayType.stringOf(het), serializeToJsonWriter(objectArrayType, het, jsc));

        final Type<ReviewBean[]> beanArrayType = TypeFactory.getType(ReviewBean[].class);
        assertEquals("[{\"name\": \"q\", \"age\": 1}, null]", serializeToJsonWriter(beanArrayType, new ReviewBean[] { new ReviewBean("q", 1), null }, jsc));

        final Type<Object[][]> nestedType = TypeFactory.getType(Object[][].class);
        assertEquals("[[1, \"a\"], null]", serializeToJsonWriter(nestedType, new Object[][] { { 1, "a" }, null }, jsc));

        assertEquals("[]", serializeToJsonWriter(objectArrayType, new Object[0], jsc));
        assertEquals("null", serializeToJsonWriter(objectArrayType, null, jsc));

        // serializable element types keep the element-by-element path
        assertEquals("[\"a\", null]", serializeToJsonWriter(stringArrayType, new String[] { "a", null }, jsc));
        assertEquals("[1, null]", serializeToJsonWriter(intArrayType, new Integer[] { 1, null }, jsc));
        assertEquals("[\"a\", \"\"]", serializeToJsonWriter(stringArrayType, new String[] { "a", null }, JsonSerConfig.create().setWriteNullStringAsEmpty(true)));
    }

    @Test
    public void reviewFixes20260906_serializeToWithNullConfigKeepsElementByElementForm() throws IOException {
        final Object[] het = { 1, "a", null, 2.5, true, new ReviewBean("z", 3) };

        assertEquals("[1, a, null, 2.5, true, {\\\"name\\\": \\\"z\\\", \\\"age\\\": 3}]", serializeToJsonWriter(objectArrayType, het, null));
        assertEquals("null", serializeToJsonWriter(objectArrayType, null, null));
        assertEquals("[]", serializeToJsonWriter(objectArrayType, new Object[0], null));
    }

    @Test
    public void reviewFixes20260906_serializeToWithXmlConfigWritesEscapedJsonTextNeverRawJson() throws IOException {
        final Object[] het = { 1, "a\"b", null, "x<y&z", new ReviewBean("z", 3) };

        // the JSON text of stringOf(x), escaped for XML character content (no raw ", < or &)
        final String expected = "[1, &quot;a\\&quot;b&quot;, null, &quot;x&lt;y&amp;z&quot;, {&quot;name&quot;: &quot;z&quot;, &quot;age&quot;: 3}]";
        assertEquals(expected, serializeToXmlWriter(objectArrayType, het, XmlSerConfig.create()));

        // a JSON config on a non-JSON writer must also go through writeCharacter (never hand an XML writer to the JSON parser)
        assertEquals(expected, serializeToXmlWriter(objectArrayType, het, JsonSerConfig.create()));

        assertEquals("null", serializeToXmlWriter(objectArrayType, null, XmlSerConfig.create()));
        assertEquals("[]", serializeToXmlWriter(objectArrayType, new Object[0], XmlSerConfig.create()));
    }

    @Test
    public void reviewFixes20260906_jsonParserRoundTripsWrapperPropertiesWithTypedElements() {
        final JsonParser jp = ParserFactory.createJsonParser();
        final Object[] het = { 1, "a", null, 2.5, true, new ReviewBean("z", 3) };
        final ReviewHolder h = new ReviewHolder();
        h.setOptObj(Optional.of(het));
        h.setOptBean(Optional.of(new ReviewBean[] { new ReviewBean("q", 1), null }));
        h.setTup(Tuple.of(het, "x"));
        h.setPair(Pair.of(het, "x"));
        h.setPlain(het);

        final String hetJson = "[1, \"a\", null, 2.5, true, {\"name\": \"z\", \"age\": 3}]";
        final String json = jp.serialize(h);
        assertEquals("{\"optObj\": " + hetJson + ", \"optBean\": [{\"name\": \"q\", \"age\": 1}, null], \"tup\": [" + hetJson + ", \"x\"], \"pair\": [" + hetJson
                + ", \"x\"], \"plain\": " + hetJson + "}", json);

        final ReviewHolder back = jp.deserialize(json, ReviewHolder.class);
        assertHeterogeneousElements(back.getOptObj().get(), "optObj");
        assertHeterogeneousElements(back.getTup()._1, "tup");
        assertHeterogeneousElements(back.getPair().left(), "pair");
        assertHeterogeneousElements(back.getPlain(), "plain");
        assertEquals("x", back.getTup()._2);
        assertEquals("x", back.getPair().right());
        assertEquals(2, back.getOptBean().get().length);
        assertEquals("q", back.getOptBean().get()[0].getName());
        assertEquals(1, back.getOptBean().get()[0].getAge());
        assertNull(back.getOptBean().get()[1]);

        // pretty format: the nested array is still real JSON and parses back with the same element classes
        final ReviewHolder pretty = jp.deserialize(jp.serialize(h, JsonSerConfig.create().setPrettyFormat(true)), ReviewHolder.class);
        assertHeterogeneousElements(pretty.getOptObj().get(), "pretty optObj");
        assertHeterogeneousElements(pretty.getPair().left(), "pretty pair");

        // empty / absent wrappers
        final ReviewHolder edge = new ReviewHolder();
        edge.setOptObj(Optional.of(new Object[0]));
        edge.setOptBean(Optional.empty());
        assertEquals("{\"optObj\": [], \"optBean\": null}", jp.serialize(edge));
        assertEquals(0, jp.deserialize("{\"optObj\": []}", ReviewHolder.class).getOptObj().get().length);
    }

    @Test
    public void reviewFixes20260906_xmlParserRoundTripsWrapperPropertiesIncludingQuotedAndCommaStrings() {
        final XmlParser xp = ParserFactory.createXmlParser();
        final Object[] het = { 1, "a\"b", null, 2.5, true, "x<y&z", "p,q", new ReviewBean("z", 3) };
        final ReviewHolder h = new ReviewHolder();
        h.setOptObj(Optional.of(het));
        h.setOptBean(Optional.of(new ReviewBean[] { new ReviewBean("q", 1), null }));

        final String xml = xp.serialize(h);
        // escaped JSON text (a string element is quoted, so ", < & and , survive the round trip); the bean-array text is unchanged
        assertTrue(xml.contains("<optObj>[1, &quot;a\\&quot;b&quot;, null, 2.5, true, &quot;x&lt;y&amp;z&quot;, &quot;p,q&quot;, {&quot;name&quot;: &quot;z&quot;, &quot;age&quot;: 3}]</optObj>"), xml);
        assertTrue(xml.contains("<optBean>[{&quot;name&quot;: &quot;q&quot;, &quot;age&quot;: 1}, null]</optBean>"), xml);

        final ReviewHolder back = xp.deserialize(xml, ReviewHolder.class);
        final Object[] a = back.getOptObj().get();
        assertEquals(8, a.length);
        assertEquals(Integer.valueOf(1), a[0]);
        assertEquals("a\"b", a[1]);
        assertNull(a[2]);
        assertEquals(Double.valueOf(2.5), a[3]);
        assertEquals(Boolean.TRUE, a[4]);
        assertEquals("x<y&z", a[5]);
        assertEquals("p,q", a[6]);
        assertEquals("z", ((Map<?, ?>) a[7]).get("name"));
        assertEquals("q", back.getOptBean().get()[0].getName());
        assertNull(back.getOptBean().get()[1]);
    }

    @Test
    public void reviewFixes20260906_valueOfRejectsElementsThatCannotBeStoredInTheDeclaredArray() {
        final JsonParser jp = ParserFactory.createJsonParser();

        // Before the fixes an ArrayList[] / HashMap[] came back (ClassCastException at the caller) or ArrayStoreException.
        // The JSON reader now hands a scalar slot the raw text of a nested value, so the result is a real String[]
        // holding that text (the parser-side collectionToArray guard is pinned by the XML test below).
        final String[][] cases = { { "[[1]]", "[1]" }, { "[{}]", "{}" }, { "[{\"a\": 1}]", "{\"a\": 1}" }, { "[a, [1]]", "a", "[1]" }, { "[[1], a]", "[1]", "a" },
                { "[null, [1]]", null, "[1]" }, { "[[]]", "[]" } };

        for (final String[] c : cases) {
            final String[] expected = Arrays.copyOfRange(c, 1, c.length);
            final String[] viaType = stringArrayType.valueOf(c[0]);
            assertEquals(String[].class, viaType.getClass(), c[0]);
            assertArrayEquals(expected, viaType, c[0]);
            assertArrayEquals(expected, jp.deserialize(c[0], String[].class), c[0]);
        }

        // valid inputs and assignable element types are unchanged
        assertArrayEquals(new String[] { "a", null }, stringArrayType.valueOf("[\"a\", null]"));
        assertEquals(0, stringArrayType.valueOf("[]").length);

        final Object[] objects = objectArrayType.valueOf("[[1], {}, \"s\", 2]");
        assertTrue(objects[0] instanceof List, String.valueOf(objects[0]));
        assertEquals(1, ((List<?>) objects[0]).get(0));
        assertTrue(objects[1] instanceof Map, String.valueOf(objects[1]));
        assertEquals("s", objects[2]);
        assertEquals(2, objects[3]);

        final Type<java.io.Serializable[]> serializableArrayType = TypeFactory.getType(java.io.Serializable[].class);
        final java.io.Serializable[] mixed = serializableArrayType.valueOf("[1, \"a\", null]");
        assertEquals(Integer.valueOf(1), mixed[0]);
        assertEquals("a", mixed[1]);
        assertNull(mixed[2]);

        assertThrows(NumberFormatException.class, () -> intArrayType.valueOf("[[1]]"));
    }

    @Test
    public void reviewFixes20260906_xmlParserRejectsNestedElementInStringArray() {
        final XmlParser xp = ParserFactory.createXmlParser();

        // Before the fix a HashMap[] came back for a String[] target (heap pollution: ClassCastException at the
        // caller). The declared element type is now honoured on the XML path too - a nested element is read as its
        // text content, exactly as the JSON path reads a nested value into a String element.
        final String[] nested = xp.deserialize("<array><e><list><e>1</e></list></e></array>", String[].class);
        assertArrayEquals(new String[] { "1" }, nested);
        assertEquals(String.class, nested.getClass().getComponentType());
        assertArrayEquals(new String[] { "a", "b" }, xp.deserialize("<array><e>a</e><e>b</e></array>", String[].class));

        assertArrayEquals(new String[] { "a", "b" }, xp.deserialize(xp.serialize(new String[] { "a", "b" }), String[].class));
        assertArrayEquals(new String[] { "a", null }, xp.deserialize(xp.serialize(new String[] { "a", null }), String[].class));
    }

    @Test
    public void reviewFixes20260906_valueOfIgnoresUnicodeStructuralWhitespaceAroundTheArray() {
        // U+2003 EM SPACE and U+3000 IDEOGRAPHIC SPACE are Character.isWhitespace and were rejected only by ObjectArrayType
        assertArrayEquals(new String[] { "a", "b" }, stringArrayType.valueOf("\u2003[\"a\", \"b\"]\u3000"));
        assertArrayEquals(new Integer[] { 1, 2 }, intArrayType.valueOf("\u3000[1, 2]\u2003"));
        assertArrayEquals(new Integer[] { 1, 2 }, intArrayType.valueOf(" \t[1, 2]\n"));
        assertArrayEquals(new Object[] { 1, "a" }, objectArrayType.valueOf("\u2003 [1, \"a\"] \u2003\n"));

        final UUID u = UUID.randomUUID();
        final Type<UUID[]> uuidArrayType = TypeFactory.getType(UUID[].class);
        assertArrayEquals(new UUID[] { u }, uuidArrayType.valueOf("\u3000[\"" + u + "\"]\n"));

        assertEquals(0, stringArrayType.valueOf("\u3000[]\u2003").length);
        assertEquals(0, stringArrayType.valueOf(" [] ").length);
        assertNull(stringArrayType.valueOf("\u3000"));
        assertNull(stringArrayType.valueOf(" \t\n"));

        // U+00A0 NO-BREAK SPACE is not Character.isWhitespace: still rejected, as it is by the primitive array types
        assertThrows(ParsingException.class, () -> stringArrayType.valueOf("[\"a\"]\u00A0"));
        assertThrows(NumberFormatException.class, () -> TypeFactory.getType(int[].class).valueOf("\u00A0[1]"));
    }

    @Test
    public void reviewFixes20260908_serializeToWritesTheEmbeddedJsonCompactlyEvenUnderAPrettyConfig() throws IOException {
        final Object[] het = { 1, "a", null, new ReviewBean("z", 3) };
        final JsonSerConfig compact = JsonSerConfig.create();
        final JsonSerConfig pretty = JsonSerConfig.create().setPrettyFormat(true);

        // serializeTo is not told the caller's current indentation, so a pretty embedded array would restart at the
        // left margin and mis-align every one of its lines - the same rule AbstractTupleType.serializeSlot applies.
        final String expected = "[1, \"a\", null, {\"name\": \"z\", \"age\": 3}]";
        assertEquals(expected, serializeToJsonWriter(objectArrayType, het, compact));
        assertEquals(expected, serializeToJsonWriter(objectArrayType, het, pretty));
        assertFalse(serializeToJsonWriter(objectArrayType, het, pretty).contains("\n"));

        // the escaped-text form written to a non-JSON writer must not carry the line breaks either
        assertEquals(serializeToXmlWriter(objectArrayType, het, compact), serializeToXmlWriter(objectArrayType, het, pretty));
        assertFalse(serializeToXmlWriter(objectArrayType, het, pretty).contains("&#xa;"));

        // the caller's config is copied, never mutated
        assertTrue(pretty.isPrettyFormat());

        final Type<ReviewBean[]> beanArrayType = TypeFactory.getType(ReviewBean[].class);
        assertEquals("[{\"name\": \"q\", \"age\": 1}, null]",
                serializeToJsonWriter(beanArrayType, new ReviewBean[] { new ReviewBean("q", 1), null }, pretty));
    }
}
