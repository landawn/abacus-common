package com.landawn.abacus.util;

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

public class Objectory100Test extends TestBase {

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
    public void testCreateSet() {
        Set<String> set = Objectory.createSet();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set.isEmpty());

        set.add("test");
        Assertions.assertTrue(set.contains("test"));
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
    public void testCreateMap() {
        Map<String, Integer> map = Objectory.createMap();
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map.isEmpty());

        map.put("key", 100);
        Assertions.assertEquals(100, map.get("key"));
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
    public void testCreateObjectArray() {
        Object[] array = Objectory.createObjectArray();
        Assertions.assertNotNull(array);
        // Assertions.assertEquals(Objectory.POOLABLE_ARRAY_LENGTH, array.length);
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
        // int largeSize = Objectory.POOLABLE_ARRAY_LENGTH + 100;
        int largeSize = 128 + 100;
        Object[] array = Objectory.createObjectArray(largeSize);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(largeSize, array.length);
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
    public void testCreateBufferedWriterWithWriter() throws Exception {
        StringWriter writer = new StringWriter();
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(writer);
        Assertions.assertNotNull(bw);

        bw.write("Test");
        bw.flush();
        Assertions.assertEquals("Test", writer.toString());
    }

    @Test
    public void testCreateBufferedWriterAlreadyBuffered() {
        java.io.BufferedWriter existing = new java.io.BufferedWriter(new StringWriter());
        java.io.BufferedWriter bw = Objectory.createBufferedWriter(existing);
        Assertions.assertSame(existing, bw);
    }

    @Test
    public void testCreateBufferedXMLWriter() {
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedXMLWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedXMLWriterWithWriter() {
        StringWriter writer = new StringWriter();
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter(writer);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedJSONWriter() {
        BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedJSONWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedJSONWriter bw = Objectory.createBufferedJSONWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedJSONWriterWithWriter() {
        StringWriter writer = new StringWriter();
        BufferedJSONWriter bw = Objectory.createBufferedJSONWriter(writer);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedCSVWriter() {
        BufferedCSVWriter bw = Objectory.createBufferedCSVWriter();
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedCSVWriterWithOutputStream() {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BufferedCSVWriter bw = Objectory.createBufferedCSVWriter(os);
        Assertions.assertNotNull(bw);
    }

    @Test
    public void testCreateBufferedCSVWriterWithWriter() {
        StringWriter writer = new StringWriter();
        BufferedCSVWriter bw = Objectory.createBufferedCSVWriter(writer);
        Assertions.assertNotNull(bw);
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
    public void testCreateBufferedReaderAlreadyBuffered() {
        java.io.BufferedReader existing = new java.io.BufferedReader(new StringReader("test"));
        java.io.BufferedReader reader = Objectory.createBufferedReader(existing);
        Assertions.assertSame(existing, reader);
    }

    @Test
    public void testRecycleList() {
        List<String> list = Objectory.createList();
        list.add("test");

        Objectory.recycle(list);
        // After recycle, list should be cleared but still usable
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testRecycleNullList() {
        // Should not throw
        Objectory.recycle((List<?>) null);
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
    public void testRecycleNullObjectArray() {
        // Should not throw
        Objectory.recycle((Object[]) null);
    }

    @Test
    public void testRecycleLargeObjectArray() {
        Object[] array = Objectory.createObjectArray(128 + 100);
        array[0] = "test";

        // Large arrays should not be recycled
        Objectory.recycle(array);
        Assertions.assertEquals("test", array[0]);
    }

    @Test
    public void testRecycleCharArray() {
        char[] buffer = Objectory.createCharArrayBuffer();
        buffer[0] = 'A';

        Objectory.recycle(buffer);
        // Buffer is returned to pool, content not cleared
    }

    @Test
    public void testRecycleByteArray() {
        byte[] buffer = Objectory.createByteArrayBuffer();
        buffer[0] = 65;

        Objectory.recycle(buffer);
        // Buffer is returned to pool, content not cleared
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
    public void testRecycleBufferedXMLWriter() {
        BufferedXMLWriter bw = Objectory.createBufferedXMLWriter();
        Objectory.recycle(bw);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedJSONWriter() {
        BufferedJSONWriter bw = Objectory.createBufferedJSONWriter();
        Objectory.recycle(bw);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedCSVWriter() {
        BufferedCSVWriter bw = Objectory.createBufferedCSVWriter();
        Objectory.recycle(bw);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedWriter() {
        java.io.BufferedWriter writer = Objectory.createBufferedWriter();
        Objectory.recycle(writer);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedWriterJSON() {
        BufferedJSONWriter writer = Objectory.createBufferedJSONWriter();
        Objectory.recycle((java.io.BufferedWriter) writer);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedWriterXML() {
        BufferedXMLWriter writer = Objectory.createBufferedXMLWriter();
        Objectory.recycle((java.io.BufferedWriter) writer);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedWriterCSV() {
        BufferedCSVWriter writer = Objectory.createBufferedCSVWriter();
        Objectory.recycle((java.io.BufferedWriter) writer);
        // Should not throw
    }

    @Test
    public void testRecycleBufferedReader() throws Exception {
        java.io.BufferedReader reader = Objectory.createBufferedReader("test");
        Objectory.recycle(reader);
        // Should not throw
    }

    @Test
    public void testRecycleRegularBufferedReader() {
        // Regular BufferedReader (not our custom one) should not be recycled
        java.io.BufferedReader reader = new java.io.BufferedReader(new StringReader("test"));
        Objectory.recycle(reader);
        // Should not throw, but won't be pooled
    }

    @Test
    public void testPoolReuse() {
        // Test that objects are actually reused from the pool
        StringBuilder sb1 = Objectory.createStringBuilder();
        sb1.append("test");
        Objectory.recycle(sb1);

        StringBuilder sb2 = Objectory.createStringBuilder();
        // Due to pooling, sb2 might be the same instance as sb1 (after clearing)
        Assertions.assertEquals(0, sb2.length());
    }
}