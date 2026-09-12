package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class CollectionTypeTest extends TestBase {

    private CollectionType<String, List<String>> listType;
    private CollectionType<Integer, Set<Integer>> setType;
    private CollectionType<Object, Queue<Object>> queueType;
    private CharacterWriter writer;

    @BeforeEach
    public void setUp() {
        listType = (CollectionType<String, List<String>>) createType("List<String>");
        setType = (CollectionType<Integer, Set<Integer>>) createType("Set<Integer>");
        queueType = (CollectionType<Object, Queue<Object>>) createType("Queue<Object>");
        writer = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String result = listType.declaringName();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("List"));
        Assertions.assertTrue(result.contains("String"));
    }

    @Test
    public void testClazz() {
        Class<List<String>> result = listType.javaType();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testGetElementType() {
        Type<String> elementType = listType.elementType();
        Assertions.assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = listType.parameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsList() {
        Assertions.assertTrue(listType.isList());
        Assertions.assertFalse(setType.isList());
        Assertions.assertFalse(queueType.isList());
    }

    @Test
    public void testIsSet() {
        Assertions.assertFalse(listType.isSet());
        Assertions.assertTrue(setType.isSet());
        Assertions.assertFalse(queueType.isSet());
    }

    @Test
    public void testIsCollection() {
        Assertions.assertTrue(listType.isCollection());
        Assertions.assertTrue(setType.isCollection());
        Assertions.assertTrue(queueType.isCollection());
    }

    @Test
    public void testIsGenericType() {
        Assertions.assertTrue(listType.isParameterizedType());
        Assertions.assertTrue(setType.isParameterizedType());
        Assertions.assertTrue(queueType.isParameterizedType());
    }

    @Test
    public void testIsSerializable() {
        boolean result = listType.isSerializable();
        Assertions.assertTrue(result);
    }

    @Test
    public void testGetSerializationType() {
        CollectionType.SerializationType result = listType.serializationType();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testRoundTrip() {
        List<String> original = Arrays.asList("one", "two", "three");
        String json = listType.stringOf(original);
        List<String> restored = listType.valueOf(json);

        assertEquals(original.size(), restored.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i), restored.get(i));
        }
    }

    @Test
    public void testStringOf_Null() {
        String result = listType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOf_EmptyList() {
        List<String> empty = new ArrayList<>();
        String result = listType.stringOf(empty);
        assertEquals("[]", result);
    }

    @Test
    public void testStringOf_SingleElement() {
        List<String> list = Arrays.asList("hello");
        String result = listType.stringOf(list);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("hello"));
        Assertions.assertTrue(result.startsWith("["));
        Assertions.assertTrue(result.endsWith("]"));
    }

    @Test
    public void testStringOf_MultipleElements() {
        List<String> list = Arrays.asList("one", "two", "three");
        String result = listType.stringOf(list);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("one"));
        Assertions.assertTrue(result.contains("two"));
        Assertions.assertTrue(result.contains("three"));
    }

    @Test
    public void testStringOf_WithNullElement() {
        List<String> list = Arrays.asList("first", null, "third");
        String result = listType.stringOf(list);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("first"));
        Assertions.assertTrue(result.contains("null"));
        Assertions.assertTrue(result.contains("third"));
    }

    @Test
    public void testSetType() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        String stringRep = setType.stringOf(set);
        Assertions.assertNotNull(stringRep);

        Set<Integer> restored = setType.valueOf(stringRep);
        Assertions.assertNotNull(restored);
        assertEquals(set.size(), restored.size());
        Assertions.assertTrue(restored.containsAll(set));
    }

    @Test
    public void testQueueType() {
        Queue<Object> queue = new LinkedList<>();
        queue.offer("first");
        queue.offer("second");

        String stringRep = queueType.stringOf(queue);
        Assertions.assertNotNull(stringRep);

        Queue<Object> restored = queueType.valueOf(stringRep);
        Assertions.assertNotNull(restored);
        assertEquals(2, restored.size());
    }

    @Test
    public void testValueOf_Null() {
        List<String> result = listType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        List<String> result = listType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyArray() {
        List<String> result = listType.valueOf("[]");
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testValueOf_SingleElement() {
        List<String> result = listType.valueOf("[\"hello\"]");
        Assertions.assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    public void testValueOf_MultipleElements() {
        List<String> result = listType.valueOf("[\"a\",\"b\",\"c\"]");
        Assertions.assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testAppendTo_Null() throws IOException {
        StringWriter sw = new StringWriter();
        listType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testAppendTo_Empty() throws IOException {
        StringWriter sw = new StringWriter();
        List<String> empty = new ArrayList<>();
        listType.appendTo(sw, empty);
        assertEquals("[]", sw.toString());
    }

    @Test
    public void testAppendTo_Elements() throws IOException {
        StringWriter sw = new StringWriter();
        List<String> list = Arrays.asList("x", "y");
        listType.appendTo(sw, list);
        String result = sw.toString();
        Assertions.assertTrue(result.startsWith("["));
        Assertions.assertTrue(result.endsWith("]"));
        Assertions.assertTrue(result.contains("x"));
        Assertions.assertTrue(result.contains("y"));
    }

    @Test
    public void testAppendTo_Writer() throws IOException {
        Writer mockWriter = mock(Writer.class);
        List<String> list = Arrays.asList("test");
        listType.appendTo(mockWriter, list);
        assertNotNull(list);
    }

    @Test
    public void testAppendTo_PropagatesWriterIOException() {
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

        assertSame(failure, assertThrows(IOException.class, () -> listType.appendTo(writer, List.of("test"))));
    }

    @Test
    public void testSerializeTo_Null() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        listType.serializeTo(mockWriter, null, null);
        verify(mockWriter).write("null".toCharArray());
    }

    @Test
    public void testSerializeTo_Empty() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        List<String> empty = new ArrayList<>();
        listType.serializeTo(mockWriter, empty, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
    }

    @Test
    public void testSerializeTo_Elements() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        List<String> list = Arrays.asList("a", "b");
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        listType.serializeTo(mockWriter, list, config);

        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
        verify(mockWriter, atLeastOnce()).write(any(char[].class));
    }

    @Test
    public void testAppendTo_unquotedToStringForm() throws IOException {
        StringBuilder sb = new StringBuilder();
        listType.appendTo(sb, Arrays.asList("a", "b"));
        // appendTo emits the plain, toString()-style form: string elements are NOT quoted
        assertEquals("[a, b]", sb.toString());
    }

    @Test
    public void testSerializeTo_jsonQuotedForm() throws IOException {
        List<String> list = Arrays.asList("a", "b");
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        listType.serializeTo(writer, list, com.landawn.abacus.parser.JsonSerConfig.create());
        String json = writer.toString();
        com.landawn.abacus.util.Objectory.recycle(writer);

        // serializeTo emits JSON: string elements ARE quoted; equals stringOf and differs from appendTo
        assertEquals("[\"a\", \"b\"]", json);
        assertEquals(listType.stringOf(list), json);

        StringBuilder sb = new StringBuilder();
        listType.appendTo(sb, list);
        org.junit.jupiter.api.Assertions.assertNotEquals(sb.toString(), json);
    }

    @Test
    public void testSerializeTo_NullElementHonorsElementConfig() throws IOException {
        com.landawn.abacus.util.BufferedJsonWriter actualWriter = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            listType.serializeTo(actualWriter, Arrays.asList((String) null), com.landawn.abacus.parser.JsonSerConfig.create().setWriteNullStringAsEmpty(true));
            assertEquals("[\"\"]", actualWriter.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(actualWriter);
        }
    }

    private static String serializeToJson(final Type<?> type, final Object x, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws IOException {
        final com.landawn.abacus.util.BufferedJsonWriter w = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();

        try {
            ((Type<Object>) type).serializeTo(w, x, config);
            return w.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(w);
        }
    }

    private static String serializeToXml(final Type<?> type, final Object x, final com.landawn.abacus.parser.JsonXmlSerConfig<?> config) throws IOException {
        final com.landawn.abacus.util.BufferedXmlWriter w = com.landawn.abacus.util.Objectory.createBufferedXmlWriter();

        try {
            ((Type<Object>) type).serializeTo(w, x, config);
            return w.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(w);
        }
    }

    public static class OptListBean {
        private com.landawn.abacus.util.u.Optional<List<Object>> optList;
        private List<Object> plain;

        public com.landawn.abacus.util.u.Optional<List<Object>> getOptList() {
            return optList;
        }

        public void setOptList(final com.landawn.abacus.util.u.Optional<List<Object>> optList) {
            this.optList = optList;
        }

        public List<Object> getPlain() {
            return plain;
        }

        public void setPlain(final List<Object> plain) {
            this.plain = plain;
        }
    }

    // VT7b T7-01 (CollectionType sibling): serializeTo quoted every element of a List<Object> under a JSON config
    // (["1", "a", ...] -> read back as Strings). Now the JSON parser writes the structural form.
    @Test
    public void reviewFixes20260906_serializeToWritesStructuralJsonForNonSerializableElementType() throws IOException {
        final Type<List<Object>> objList = TypeFactory.getType("List<Object>");
        final List<Object> mixed = Arrays.asList(1, "a", null, 2.5, true);
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        assertEquals("[1, \"a\", null, 2.5, true]", serializeToJson(objList, mixed, jsc));
        assertEquals(objList.stringOf(mixed), serializeToJson(objList, mixed, jsc));
        assertEquals("[{\"k\": 1}, [1, 2], \"中\"]", serializeToJson(objList, Arrays.asList(com.landawn.abacus.util.N.asMap("k", 1), Arrays.asList(1, 2), "中"), jsc));
        assertEquals("[]", serializeToJson(objList, new ArrayList<>(), jsc));
        assertEquals("null", serializeToJson(objList, null, jsc));

        // unchanged: no config at all keeps the per-element (unquoted) form
        assertEquals("[1, a, null, 2.5, true]", serializeToJson(objList, mixed, null));

        // changed by FINDING 20/88/121: an XML config no longer goes element by element (which dropped the String
        // quoting and produced text stringOf/valueOf cannot read back); the stringOf text is escaped for XML instead.
        final com.landawn.abacus.util.BufferedXmlWriter xw = com.landawn.abacus.util.Objectory.createBufferedXmlWriter();
        try {
            objList.serializeTo(xw, Arrays.asList(1, "a<&\"", null), com.landawn.abacus.parser.XmlSerConfig.create());
            assertEquals("[1, &quot;a&lt;&amp;\\&quot;&quot;, null]", xw.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(xw);
        }

        // unchanged: a serializable element type still takes the typed per-element path
        assertEquals("[\"a\", null]", serializeToJson(listType, Arrays.asList("a", null), jsc));
        assertEquals("[1, 2]", serializeToJson(TypeFactory.getType("List<Integer>"), Arrays.asList(1, 2), jsc));
    }

    // A JSON config on a non-JSON writer must never receive raw JSON: the text goes through writeCharacter so it is
    // escaped for that format (same guard as ObjectArrayType.serializeTo).
    @Test
    public void reviewFixes20260906_jsonConfigOnANonJsonWriterWritesEscapedTextNeverRawJson() throws IOException {
        final Type<List<Object>> objList = TypeFactory.getType("List<Object>");
        final List<Object> mixed = Arrays.asList(1, "a\"b", null, "x<y&z", "p,q");
        final com.landawn.abacus.parser.JsonSerConfig jsc = com.landawn.abacus.parser.JsonSerConfig.create();

        final com.landawn.abacus.util.BufferedXmlWriter xw = com.landawn.abacus.util.Objectory.createBufferedXmlWriter();
        try {
            objList.serializeTo(xw, mixed, jsc);
            assertEquals("[1, &quot;a\\&quot;b&quot;, null, &quot;x&lt;y&amp;z&quot;, &quot;p,q&quot;]", xw.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(xw);
        }

        final com.landawn.abacus.util.BufferedCsvWriter cw = com.landawn.abacus.util.Objectory.createBufferedCsvWriter();
        try {
            objList.serializeTo(cw, mixed, jsc);
            assertEquals("[1, \"\"a\\\"\"b\"\", null, \"\"x<y&z\"\", \"\"p,q\"\"]", cw.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(cw);
        }

        // the sibling array handler produces the same text for the same values
        final Type<Object[]> objArray = TypeFactory.getType(Object[].class);
        final com.landawn.abacus.util.BufferedXmlWriter xw2 = com.landawn.abacus.util.Objectory.createBufferedXmlWriter();
        try {
            objArray.serializeTo(xw2, mixed.toArray(), jsc);
            assertEquals("[1, &quot;a\\&quot;b&quot;, null, &quot;x&lt;y&amp;z&quot;, &quot;p,q&quot;]", xw2.toString());
        } finally {
            com.landawn.abacus.util.Objectory.recycle(xw2);
        }
    }

    @Test
    public void reviewFixes20260906_optionalListOfObjectPropertyRoundTripsWithTypedElements() {
        final OptListBean bean = new OptListBean();
        final List<Object> mixed = Arrays.asList(1, "a", null, 2.5, true);
        bean.setOptList(com.landawn.abacus.util.u.Optional.of(mixed));
        bean.setPlain(mixed);

        final String json = com.landawn.abacus.util.N.toJson(bean);
        assertEquals("{\"optList\": [1, \"a\", null, 2.5, true], \"plain\": [1, \"a\", null, 2.5, true]}", json);

        final OptListBean back = com.landawn.abacus.util.N.fromJson(json, OptListBean.class);
        final List<Object> ol = back.getOptList().get();
        assertEquals(mixed, ol);
        assertEquals(Integer.class, ol.get(0).getClass());
        assertEquals(String.class, ol.get(1).getClass());
        Assertions.assertNull(ol.get(2));
        assertEquals(Double.class, ol.get(3).getClass());
        assertEquals(Boolean.class, ol.get(4).getClass());

        // XML output unchanged and still round-trips
        final String xml = com.landawn.abacus.util.N.toXml(bean);
        Assertions.assertTrue(xml.contains("<optList>[1, &quot;a&quot;, null, 2.5, true]</optList>"), xml);
        assertEquals(mixed, com.landawn.abacus.util.N.fromXml(xml, OptListBean.class).getOptList().get());
    }

    // T8-06 (documented contract): an element that is not compatible with the declared element type fails with a
    // ClassCastException; numeric narrowing across boxes stays.
    @Test
    public void reviewFixes20260906_incompatibleElementThrowsClassCastException() throws IOException {
        final Type<List<Integer>> intList = TypeFactory.getType("List<Integer>");
        final List<Integer> polluted = (List) Arrays.asList("1", 2L);

        assertThrows(ClassCastException.class, () -> intList.stringOf(polluted));
        assertThrows(ClassCastException.class, () -> serializeToJson(intList, polluted, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertThrows(ClassCastException.class, () -> intList.appendTo(new StringBuilder(), polluted));

        assertEquals("[2, 3]", intList.stringOf((List) Arrays.asList(2L, 3.0)));
        assertEquals("[\"1\", 2]", TypeFactory.getType("List<Object>").stringOf(Arrays.asList("1", 2)));
    }

    public static class MyBag<E> extends java.util.AbstractCollection<E> {
        public MyBag(final int capacity) {
        }

        @Override
        public java.util.Iterator<E> iterator() {
            return java.util.Collections.emptyIterator();
        }

        @Override
        public int size() {
            return 0;
        }
    }

    // T8-12 / T8-14 (documented failure modes of valueOf)
    @Test
    public void reviewFixes20260906_valueOfDocumentedFailureModes() {
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> listType.valueOf("[\"a\", \"b\""));
        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> setType.valueOf("[1, 2"));

        final Type<MyBag<Integer>> bagType = TypeFactory.getType("com.landawn.abacus.type.CollectionTypeTest$MyBag<Integer>");
        assertThrows(IllegalArgumentException.class, () -> bagType.valueOf("[]"));
        assertThrows(IllegalArgumentException.class, () -> bagType.valueOf("[1]"));

        Assertions.assertNull(listType.valueOf("   "));
        assertEquals(0, listType.valueOf("[]").size());
    }

    // FINDING 20 / 88 / 121: under a non-JSON config a non-serializable element type must be written the way
    // stringOf() writes it (escaped for the target format), exactly as the Object[] handler already does. Going
    // element by element dropped the String quoting, so Object[] and List<Object> disagreed on the same values and
    // the collection text could not be read back by valueOf.
    @Test
    public void reviewFixes20260908_xmlConfigWritesTheSameStringOfTextAsTheArrayHandler() throws IOException {
        final Type<List<Object>> objList = TypeFactory.getType("List<Object>");
        final Type<Object[]> objArray = TypeFactory.getType(Object[].class);
        final List<Object> mixed = Arrays.asList(1, "a<&\"", null, 2.5, true);

        final String listXml = serializeToXml(objList, mixed, com.landawn.abacus.parser.XmlSerConfig.create());
        final String arrayXml = serializeToXml(objArray, mixed.toArray(), com.landawn.abacus.parser.XmlSerConfig.create());

        assertEquals("[1, &quot;a&lt;&amp;\\&quot;&quot;, null, 2.5, true]", listXml);
        assertEquals(arrayXml, listXml);

        // it is the XML-escaped form of stringOf, i.e. text valueOf() can read back
        assertEquals("[1, \"a<&\\\"\", null, 2.5, true]", objList.stringOf(mixed));
        assertEquals(mixed, objList.valueOf(objList.stringOf(mixed)));

        // unchanged: an empty collection, a null collection, and a serializable element type
        assertEquals("[]", serializeToXml(objList, new ArrayList<>(), com.landawn.abacus.parser.XmlSerConfig.create()));
        assertEquals("null", serializeToXml(objList, null, com.landawn.abacus.parser.XmlSerConfig.create()));
        assertEquals("[a&lt;b, null]", serializeToXml(listType, Arrays.asList("a<b", null), com.landawn.abacus.parser.XmlSerConfig.create()));
    }

    // FINDING 88 sibling inside this file: the embedded JSON write must not inherit prettyFormat - this handler is
    // not told the caller's current indentation, so a pretty embedded collection restarts at the left margin (and on
    // an XML writer puts &#xa; line breaks into character content). ObjectArrayType was fixed the same way.
    @Test
    public void reviewFixes20260908_embeddedJsonIsWrittenCompactlyEvenUnderAPrettyConfig() throws IOException {
        final Type<List<Object>> objList = TypeFactory.getType("List<Object>");
        final Type<Object[]> objArray = TypeFactory.getType(Object[].class);
        final List<Object> mixed = Arrays.asList(1, "a", null, 2.5, true);
        final com.landawn.abacus.parser.JsonSerConfig pretty = com.landawn.abacus.parser.JsonSerConfig.create().setPrettyFormat(true);

        final String json = serializeToJson(objList, mixed, pretty);
        assertEquals("[1, \"a\", null, 2.5, true]", json);
        assertEquals(serializeToJson(objArray, mixed.toArray(), pretty), json);

        // the caller's config object is copied, never mutated
        Assertions.assertTrue(pretty.isPrettyFormat());

        // a config that is not pretty is passed straight through
        assertEquals(json, serializeToJson(objList, mixed, com.landawn.abacus.parser.JsonSerConfig.create()));
    }
}
