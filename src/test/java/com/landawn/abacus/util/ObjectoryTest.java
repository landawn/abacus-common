package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ObjectoryTest extends TestBase {

    @Test
    public void testCreateList() {
        List<String> list = Objectory.createList();
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());

        list.add("test");
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("test", list.get(0));
    }

    @Test
    public void testListPoolReuse() {
        List<String> list1 = Objectory.createList();
        list1.add("item1");
        list1.add("item2");
        Objectory.recycle(list1);

        List<String> list2 = Objectory.createList();
        Assertions.assertTrue(list2.isEmpty());
    }

    @Test
    public void testCreateSet() {
        Set<String> set = Objectory.createSet();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set.isEmpty());

        set.add("test");
        Assertions.assertTrue(set.contains("test"));
    }

    @Test
    public void testSetPoolReuse() {
        Set<String> set1 = Objectory.createSet();
        set1.add("a");
        set1.add("b");
        Objectory.recycle(set1);

        Set<String> set2 = Objectory.createSet();
        Assertions.assertTrue(set2.isEmpty());
    }

    @Test
    public void testCreateLinkedHashSet() {
        Set<String> set = Objectory.createLinkedHashSet();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set.isEmpty());

        set.add("first");
        set.add("second");
        Assertions.assertEquals(2, set.size());
    }

    @Test
    public void testLinkedHashSetPoolReuse() {
        Set<String> set1 = Objectory.createLinkedHashSet();
        set1.add("x");
        Objectory.recycle(set1);

        Set<String> set2 = Objectory.createLinkedHashSet();
        Assertions.assertTrue(set2.isEmpty());
    }

    @Test
    public void testCreateMap() {
        Map<String, Integer> map = Objectory.createMap();
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map.isEmpty());

        map.put("key", 100);
        Assertions.assertEquals(100, map.get("key"));
    }

    @Test
    public void testMapPoolReuse() {
        Map<String, Integer> map1 = Objectory.createMap();
        map1.put("key", 1);
        Objectory.recycle(map1);

        Map<String, Integer> map2 = Objectory.createMap();
        Assertions.assertTrue(map2.isEmpty());
    }

    @Test
    public void testCreateLinkedHashMap() {
        Map<String, Integer> map = Objectory.createLinkedHashMap();
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map.isEmpty());

        map.put("key1", 1);
        map.put("key2", 2);
        Assertions.assertEquals(2, map.size());
    }

    @Test
    public void testLinkedHashMapPoolReuse() {
        Map<String, Integer> map1 = Objectory.createLinkedHashMap();
        map1.put("a", 1);
        Objectory.recycle(map1);

        Map<String, Integer> map2 = Objectory.createLinkedHashMap();
        Assertions.assertTrue(map2.isEmpty());
    }

    @Test
    public void testCreateObjectArray() {
        Object[] array = Objectory.createObjectArray();
        Assertions.assertNotNull(array);
        Assertions.assertEquals(128, array.length);
    }

    @Test
    public void testCreateObjectArrayWithSize() {
        Object[] array = Objectory.createObjectArray(10);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(10, array.length);

        array[0] = "Hello";
        array[1] = 42;
        Assertions.assertEquals("Hello", array[0]);
        Assertions.assertEquals(42, array[1]);
    }

    @Test
    public void testCreateObjectArrayLargeSize() {
        int largeSize = 128 + 100;
        Object[] array = Objectory.createObjectArray(largeSize);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(largeSize, array.length);
    }

    @Test
    public void testObjectArrayPoolReuse() {
        Object[] arr1 = Objectory.createObjectArray(10);
        arr1[0] = "hello";
        arr1[1] = 42;
        Objectory.recycle(arr1);

        Object[] arr2 = Objectory.createObjectArray(10);
        // After recycling, entries should be nulled
        Assertions.assertNotNull(arr2);
    }

    @Test
    public void testCreateObjectArray_ZeroSize() {
        Object[] array = Objectory.createObjectArray(0);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(0, array.length);
    }

    @Test
    public void testCreateCharArrayBuffer() {
        char[] buffer = Objectory.createCharArrayBuffer();
        Assertions.assertNotNull(buffer);
        Assertions.assertEquals(Objectory.BUFFER_SIZE, buffer.length);
    }

    @Test
    public void testCreateCharArrayBufferWithCapacity() {
        char[] buffer = Objectory.createCharArrayBuffer(1024);
        Assertions.assertNotNull(buffer);
        Assertions.assertEquals(Objectory.BUFFER_SIZE, buffer.length);

        int largeCapacity = Objectory.BUFFER_SIZE + 1000;
        char[] largeBuffer = Objectory.createCharArrayBuffer(largeCapacity);
        Assertions.assertEquals(largeCapacity, largeBuffer.length);
    }

    @Test
    public void testCreateByteArrayBuffer() {
        byte[] buffer = Objectory.createByteArrayBuffer();
        Assertions.assertNotNull(buffer);
        Assertions.assertEquals(Objectory.BUFFER_SIZE, buffer.length);
    }

    @Test
    public void testCreateByteArrayBufferWithCapacity() {
        byte[] buffer = Objectory.createByteArrayBuffer(4096);
        Assertions.assertNotNull(buffer);
        Assertions.assertEquals(Objectory.BUFFER_SIZE, buffer.length);

        int largeCapacity = Objectory.BUFFER_SIZE + 1000;
        byte[] largeBuffer = Objectory.createByteArrayBuffer(largeCapacity);
        Assertions.assertEquals(largeCapacity, largeBuffer.length);
    }

    @Test
    public void testPoolReuse() {
        StringBuilder sb1 = Objectory.createStringBuilder();
        sb1.append("test");
        Objectory.recycle(sb1);

        StringBuilder sb2 = Objectory.createStringBuilder();
        Assertions.assertEquals(0, sb2.length());
    }

    @Test
    public void testCreateStringBuilder() {
        StringBuilder sb = Objectory.createStringBuilder();
        Assertions.assertNotNull(sb);
        Assertions.assertTrue(sb.capacity() >= Objectory.BUFFER_SIZE);

        sb.append("Hello").append(" ").append("World");
        Assertions.assertEquals("Hello World", sb.toString());
    }

    @Test
    public void testCreateStringBuilderWithCapacity() {
        StringBuilder sb = Objectory.createStringBuilder(100);
        Assertions.assertNotNull(sb);

        int largeCapacity = Objectory.BUFFER_SIZE + 1000;
        StringBuilder largeSb = Objectory.createStringBuilder(largeCapacity);
        Assertions.assertTrue(largeSb.capacity() >= largeCapacity);
    }

    @Test
    public void testByteArrayOutputStreamPoolReuse() {
        com.landawn.abacus.util.ByteArrayOutputStream baos1 = Objectory.createByteArrayOutputStream();
        baos1.write("data".getBytes(), 0, 4);
        Objectory.recycle(baos1);

        com.landawn.abacus.util.ByteArrayOutputStream baos2 = Objectory.createByteArrayOutputStream();
        Assertions.assertEquals(0, baos2.size());
    }

    @Test
    public void testCreateByteArrayOutputStream() {
        com.landawn.abacus.util.ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream();
        Assertions.assertNotNull(baos);

        baos.write("Hello".getBytes(), 0, 5);
        byte[] result = baos.toByteArray();
        Assertions.assertEquals("Hello", new String(result));
    }

    @Test
    public void testCreateByteArrayOutputStreamWithCapacity() {
        OutputStream baos = Objectory.createByteArrayOutputStream(512);
        Assertions.assertNotNull(baos);

        int largeCapacity = Objectory.BUFFER_SIZE + 1000;
        OutputStream largeBaos = Objectory.createByteArrayOutputStream(largeCapacity);
        Assertions.assertNotNull(largeBaos);
    }

    @Test
    public void testCreateBufferedWriter() {
        java.io.BufferedWriter bw = Objectory.createBufferedWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedWriterAlreadyBuffered() {
        java.io.BufferedWriter existing = new java.io.BufferedWriter(new StringWriter());
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(existing);
        Assertions.assertSame(existing, bw);
    }

    @Test
    public void testCreateBufferedWriterWithWriter() throws Exception {
        StringWriter writer = new StringWriter();
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(writer);
        Assertions.assertNotNull(bw);

        bw.write("Test");
        bw.flush();
        Assertions.assertEquals("Test", writer.toString());
    }

    @Test
    public void testCreateBufferedWriterWithOutputStream_WriteAndRead() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(os);
        bw.write("hello world");
        bw.flush();
        Objectory.recycle(bw);

        Assertions.assertEquals("hello world", os.toString());
    }

    @Test
    public void testCreateBufferedWriter_ReusesPooledInternalWriter() throws Exception {
        java.io.BufferedWriter first = Objectory.createBufferedWriter();
        first.write("first");
        Objectory.recycle(first);

        java.io.BufferedWriter second = Objectory.createBufferedWriter();
        second.write("second");

        Assertions.assertEquals("second", second.toString());
    }

    @Test
    public void testCreateBufferedWriterWithWriter_ReusesPooledWriter() throws Exception {
        StringWriter firstTarget = new StringWriter();
        java.io.BufferedWriter first = Objectory.createBufferedWriter(firstTarget);
        first.write("first");
        Objectory.recycle(first);

        StringWriter secondTarget = new StringWriter();
        java.io.BufferedWriter second = Objectory.createBufferedWriter(secondTarget);
        second.write("second");
        second.flush();

        Assertions.assertEquals("first", firstTarget.toString());
        Assertions.assertEquals("second", secondTarget.toString());
    }

    @Test
    public void testCreateBufferedXmlWriter() {
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedXmlWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedXmlWriterWithWriter() {
        StringWriter writer = new StringWriter();
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter(writer);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedJsonWriter() {
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedJsonWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedJsonWriterWithWriter() {
        StringWriter writer = new StringWriter();
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter(writer);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedCsvWriter() {
        BufferedCsvWriter bw = Objectory.createBufferedCsvWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedCsvWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedCsvWriter bw = Objectory.createBufferedCsvWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedCsvWriterWithWriter() {
        StringWriter writer = new StringWriter();
        BufferedCsvWriter bw = Objectory.createBufferedCsvWriter(writer);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedReaderAlreadyBuffered() {
        java.io.BufferedReader existing = new java.io.BufferedReader(new StringReader("test"));
        java.io.BufferedReader reader = Objectory.createBufferedReader(existing);
        Assertions.assertSame(existing, reader);
    }

    @Test
    public void testCreateBufferedReaderWithString() throws Exception {
        {
            String text = "Line 1\nLine 2\nLine 3";
            java.io.BufferedReader reader = new BufferedReader(new StringReader(text));
            Assertions.assertNotNull(reader);

            String line = reader.readLine();
            Assertions.assertEquals("Line 1", line);
        }
        {
            String text = "Line 1\nLine 2\nLine 3";
            java.io.BufferedReader reader = Objectory.createBufferedReader(text);
            Assertions.assertNotNull(reader);

            String line = reader.readLine();
            Assertions.assertEquals("Line 1", line);
        }
    }

    @Test
    public void testCreateBufferedReaderWithInputStream() throws Exception {
        ByteArrayInputStream is = new ByteArrayInputStream("Test input".getBytes());
        java.io.BufferedReader reader = Objectory.createBufferedReader(is);
        Assertions.assertNotNull(reader);

        String line = reader.readLine();
        Assertions.assertEquals("Test input", line);
    }

    @Test
    public void testCreateBufferedReaderWithReader() throws Exception {
        StringReader stringReader = new StringReader("Test content");
        java.io.BufferedReader reader = Objectory.createBufferedReader(stringReader);
        Assertions.assertNotNull(reader);

        String line = reader.readLine();
        Assertions.assertEquals("Test content", line);
    }

    @Test
    public void testCreateBufferedReaderWithReader_ReusesPooledReader() throws Exception {
        java.io.BufferedReader first = Objectory.createBufferedReader(new StringReader("first"));
        Assertions.assertEquals("first", first.readLine());
        Objectory.recycle(first);

        java.io.BufferedReader second = Objectory.createBufferedReader(new StringReader("second"));
        Assertions.assertEquals("second", second.readLine());
        Assertions.assertNull(second.readLine());
    }

    @Test
    public void testRecycleLargeObjectArray() {
        Object[] array = Objectory.createObjectArray(128 + 100);
        array[0] = "test";

        Objectory.recycle(array);
        Assertions.assertEquals("test", array[0]);
    }

    @Test
    public void testRecycleStringBuilder() {
        StringBuilder sb = Objectory.createStringBuilder();
        sb.append("Hello World");

        Objectory.recycle(sb);
        Assertions.assertEquals(0, sb.length());
    }

    @Test
    public void testRecycleByteArrayOutputStream() {
        com.landawn.abacus.util.ByteArrayOutputStream baos = Objectory.createByteArrayOutputStream();
        baos.write("Hello".getBytes(), 0, 5);

        Objectory.recycle(baos);
        Assertions.assertEquals(0, baos.size());
    }

    @Test
    public void testRecycleList() {
        List<String> list = Objectory.createList();
        list.add("test");

        Objectory.recycle(list);
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testRecycleSet() {
        Set<String> set = Objectory.createSet();
        set.add("test");

        Objectory.recycle(set);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testRecycleLinkedHashSet() {
        Set<String> set = Objectory.createLinkedHashSet();
        set.add("test");

        Objectory.recycle(set);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testRecycleMap() {
        Map<String, Integer> map = Objectory.createMap();
        map.put("key", 100);

        Objectory.recycle(map);
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testRecycleLinkedHashMap() {
        Map<String, Integer> map = Objectory.createLinkedHashMap();
        map.put("key", 100);

        Objectory.recycle(map);
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testRecycleObjectArray() {
        Object[] array = Objectory.createObjectArray(10);
        array[0] = "Hello";
        array[1] = 42;

        Objectory.recycle(array);
        Assertions.assertNull(array[0]);
        Assertions.assertNull(array[1]);
    }

    @Test
    public void testRecycleCharArray() {
        char[] buffer = Objectory.createCharArrayBuffer();
        buffer[0] = 'A';

        Objectory.recycle(buffer);
        assertNotNull(buffer);
    }

    @Test
    public void testRecycleByteArray() {
        byte[] buffer = Objectory.createByteArrayBuffer();
        buffer[0] = 65;

        Objectory.recycle(buffer);
        assertNotNull(buffer);
    }

    @Test
    public void testRecycleBufferedXmlWriter() {
        BufferedXmlWriter bw = Objectory.createBufferedXmlWriter();
        Objectory.recycle(bw);
        assertNotNull(bw);
    }

    @Test
    public void testRecycleBufferedJsonWriter() {
        BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
        Objectory.recycle(bw);
        assertNotNull(bw);
    }

    @Test
    public void testRecycleBufferedCsvWriter() {
        BufferedCsvWriter bw = Objectory.createBufferedCsvWriter();
        Objectory.recycle(bw);
        assertNotNull(bw);
    }

    @Test
    public void testRecycleBufferedWriter() {
        java.io.BufferedWriter writer = Objectory.createBufferedWriter();
        Objectory.recycle(writer);
        assertNotNull(writer);
    }

    @Test
    public void testRecycleBufferedWriterJSON() {
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
        Objectory.recycle((java.io.BufferedWriter) writer);
        assertNotNull(writer);
    }

    @Test
    public void testRecycleBufferedWriterXML() {
        BufferedXmlWriter writer = Objectory.createBufferedXmlWriter();
        Objectory.recycle((java.io.BufferedWriter) writer);
        assertNotNull(writer);
    }

    @Test
    public void testRecycleBufferedWriterCSV() {
        BufferedCsvWriter writer = Objectory.createBufferedCsvWriter();
        Objectory.recycle((java.io.BufferedWriter) writer);
        assertNotNull(writer);
    }

    @Test
    public void testRecycleRegularBufferedReader() {
        java.io.BufferedReader reader = new java.io.BufferedReader(new StringReader("test"));
        Objectory.recycle(reader);
        assertNotNull(reader);
    }

    @Test
    public void testRecycleNullList() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((List<?>) null);
        });
    }

    @Test
    public void testRecycleNullObjectArray() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((Object[]) null);
        });
    }

    @Test
    public void testRecycleBufferedReader() throws Exception {
        java.io.BufferedReader reader = Objectory.createBufferedReader("test");
        Objectory.recycle(reader);
        assertNotNull(reader);
    }

    @Test
    public void testRecycleNullSet() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((Set<?>) null);
        });
    }

    @Test
    public void testRecycleNullMap() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((Map<?, ?>) null);
        });
    }

    @Test
    public void testRecycleNullCharArray() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((char[]) null);
        });
    }

    @Test
    public void testRecycleNullByteArray() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((byte[]) null);
        });
    }

    @Test
    public void testRecycleNullStringBuilder() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((StringBuilder) null);
        });
    }

    @Test
    public void testRecycleNullByteArrayOutputStream() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((com.landawn.abacus.util.ByteArrayOutputStream) null);
        });
    }

    @Test
    public void testRecycleNullBufferedXmlWriter() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((BufferedXmlWriter) null);
        });
    }

    @Test
    public void testRecycleNullBufferedJsonWriter() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((BufferedJsonWriter) null);
        });
    }

    @Test
    public void testRecycleNullBufferedCsvWriter() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((BufferedCsvWriter) null);
        });
    }

    @Test
    public void testRecycleLargeCharArray() {
        int largeSize = Objectory.BUFFER_SIZE + 100;
        char[] buffer = new char[largeSize];
        buffer[0] = 'Z';
        // Large char arrays are not pooled, so no error and array is unchanged
        assertDoesNotThrow(() -> Objectory.recycle(buffer));
        Assertions.assertEquals('Z', buffer[0]);
    }

    @Test
    public void testRecycleLargeByteArray() {
        int largeSize = Objectory.BUFFER_SIZE + 100;
        byte[] buffer = new byte[largeSize];
        buffer[0] = 42;
        // Large byte arrays are not pooled, so no error and array is unchanged
        assertDoesNotThrow(() -> Objectory.recycle(buffer));
        Assertions.assertEquals(42, buffer[0]);
    }

    @Test
    public void testRecycleLargeStringBuilder() {
        int largeCapacity = Objectory.BUFFER_SIZE + 100;
        StringBuilder sb = new StringBuilder(largeCapacity);
        sb.append("test");
        // Large StringBuilders are not pooled
        assertDoesNotThrow(() -> Objectory.recycle(sb));
        // sb is not cleared because it was not pooled
        Assertions.assertEquals(4, sb.length());
    }

    @Test
    public void testRecycleNullBufferedWriter() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((java.io.BufferedWriter) null);
        });
    }

    @Test
    public void testRecycleNullBufferedReader() {
        assertDoesNotThrow(() -> {
            Objectory.recycle((java.io.BufferedReader) null);
        });
    }
}
