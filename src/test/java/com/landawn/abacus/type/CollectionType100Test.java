package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class CollectionType100Test extends TestBase {

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
        Class<List<String>> result = listType.clazz();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testGetParameterTypes() {
        Type<String>[] paramTypes = listType.getParameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testGetElementType() {
        Type<String> elementType = listType.getElementType();
        Assertions.assertNotNull(elementType);
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
        Assertions.assertTrue(listType.isGenericType());
        Assertions.assertTrue(setType.isGenericType());
        Assertions.assertTrue(queueType.isGenericType());
    }

    @Test
    public void testIsSerializable() {
        boolean result = listType.isSerializable();
        Assertions.assertTrue(result);
    }

    @Test
    public void testGetSerializationType() {
        CollectionType.SerializationType result = listType.getSerializationType();
        Assertions.assertNotNull(result);
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

    }

    @Test
    public void testWriteCharacter_Null() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        listType.writeCharacter(mockWriter, null, null);
        verify(mockWriter).write("null".toCharArray());
    }

    @Test
    public void testWriteCharacter_Empty() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        List<String> empty = new ArrayList<>();
        listType.writeCharacter(mockWriter, empty, null);
        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
    }

    @Test
    public void testWriteCharacter_Elements() throws IOException {
        CharacterWriter mockWriter = createCharacterWriter();
        List<String> list = Arrays.asList("a", "b");
        JSONXMLSerializationConfig<?> config = mock(JSONXMLSerializationConfig.class);

        listType.writeCharacter(mockWriter, list, config);

        verify(mockWriter).write('[');
        verify(mockWriter).write(']');
        verify(mockWriter, atLeastOnce()).write(any(char[].class));
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
    public void testRoundTrip() {
        List<String> original = Arrays.asList("one", "two", "three");
        String json = listType.stringOf(original);
        List<String> restored = listType.valueOf(json);

        assertEquals(original.size(), restored.size());
        for (int i = 0; i < original.size(); i++) {
            assertEquals(original.get(i), restored.get(i));
        }
    }
}
