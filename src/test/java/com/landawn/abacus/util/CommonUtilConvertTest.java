package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

public class CommonUtilConvertTest extends CommonUtilTestSupport {
    @Test
    public void testConvert() {
        String result = CommonUtil.convert(123, String.class);
        assertEquals("123", result);
    }

    @Test
    public void testConvertStringToBoolean() {
        assertTrue(CommonUtil.convert("true", Boolean.class));
        assertTrue(CommonUtil.convert("TRUE", Boolean.class));
        assertTrue(CommonUtil.convert("True", Boolean.class));
        assertFalse(CommonUtil.convert("false", Boolean.class));
        assertFalse(CommonUtil.convert("FALSE", Boolean.class));
        assertFalse(CommonUtil.convert("False", Boolean.class));
        assertFalse(CommonUtil.convert("anything else", Boolean.class));
    }

    @Test
    public void testConvertNumberToNumber() {
        Integer intVal = 123;
        assertEquals(123L, CommonUtil.convert(intVal, Long.class).longValue());
        assertEquals(123.0f, CommonUtil.convert(intVal, Float.class).floatValue(), 0.000001f);
        assertEquals(123.0d, CommonUtil.convert(intVal, Double.class).doubleValue(), 0.000001d);
        assertEquals((byte) 123, CommonUtil.convert(intVal, Byte.class).byteValue());
        assertEquals((short) 123, CommonUtil.convert(intVal, Short.class).shortValue());

        Long longVal = 456L;
        assertEquals(456, CommonUtil.convert(longVal, Integer.class).intValue());
        assertEquals(456.0f, CommonUtil.convert(longVal, Float.class).floatValue(), 0.000001f);
        assertEquals(456.0d, CommonUtil.convert(longVal, Double.class).doubleValue(), 0.000001d);

        Double doubleVal = 789.5;
        assertEquals(789, CommonUtil.convert(doubleVal, Integer.class).intValue());
        assertEquals(789L, CommonUtil.convert(doubleVal, Long.class).longValue());
        assertEquals(789.5f, CommonUtil.convert(doubleVal, Float.class).floatValue(), 0.001);
    }

    @Test
    public void testConvertNumberToBoolean() {
        assertTrue(CommonUtil.convert(1, Boolean.class));
        assertTrue(CommonUtil.convert(123, Boolean.class));
        assertTrue(CommonUtil.convert(-1, Boolean.class));
        assertFalse(CommonUtil.convert(0, Boolean.class));

        assertTrue(CommonUtil.convert(1L, Boolean.class));
        assertFalse(CommonUtil.convert(0L, Boolean.class));

        assertTrue(CommonUtil.convert(1.5, Boolean.class));
        assertFalse(CommonUtil.convert(0.0, Boolean.class));
        assertTrue(CommonUtil.convert(0.9, Boolean.class));
        assertTrue(CommonUtil.convert(new BigDecimal("1e-1000"), Boolean.class));
        assertFalse(CommonUtil.convert(BigDecimal.ZERO, Boolean.class));
        assertTrue(CommonUtil.convert(BigInteger.valueOf(-1), Boolean.class));
        assertFalse(CommonUtil.convert(Double.NaN, Boolean.class));
        assertTrue(CommonUtil.convert(Double.POSITIVE_INFINITY, Boolean.class));
    }

    @Test
    public void testConvertNumberToString() {
        assertEquals("123", CommonUtil.convert(123, String.class));
        assertEquals("456", CommonUtil.convert(456L, String.class));
        assertEquals("78.9", CommonUtil.convert(78.9, String.class));
        assertEquals("true", CommonUtil.convert(true, String.class));
        assertEquals("false", CommonUtil.convert(false, String.class));
    }

    @Test
    public void testConvertCharacterToInteger() {
        assertEquals(65, CommonUtil.convert('A', Integer.class).intValue());
        assertEquals(65, CommonUtil.convert('A', int.class).intValue());
        assertEquals(97, CommonUtil.convert('a', Integer.class).intValue());
        assertEquals(48, CommonUtil.convert('0', Integer.class).intValue());
    }

    @Test
    public void testConvertIntegerToCharacter() {
        assertEquals('A', CommonUtil.convert(65, Character.class).charValue());
        assertEquals('A', CommonUtil.convert(65, char.class).charValue());
        assertEquals('a', CommonUtil.convert(97, Character.class).charValue());
        assertEquals('0', CommonUtil.convert(48, Character.class).charValue());
    }

    @Test
    public void testConvertDateToLong() {
        Date date = new Date(1234567890L);
        assertEquals(1234567890L, CommonUtil.convert(date, Long.class).longValue());
        assertEquals(1234567890L, CommonUtil.convert(date, long.class).longValue());

        java.sql.Timestamp timestamp = new java.sql.Timestamp(1234567890L);
        assertEquals(1234567890L, CommonUtil.convert(timestamp, Long.class).longValue());

        java.sql.Date sqlDate = new java.sql.Date(1234567890L);
        assertEquals(1234567890L, CommonUtil.convert(sqlDate, Long.class).longValue());
    }

    @Test
    public void testConvertLongToDate() {
        Long timeMillis = 1234567890L;

        Date date = CommonUtil.convert(timeMillis, Date.class);
        assertEquals(timeMillis.longValue(), date.getTime());

        java.sql.Timestamp timestamp = CommonUtil.convert(timeMillis, java.sql.Timestamp.class);
        assertEquals(timeMillis.longValue(), timestamp.getTime());

        java.sql.Date sqlDate = CommonUtil.convert(timeMillis, java.sql.Date.class);
        assertEquals(timeMillis.longValue(), sqlDate.getTime());

        java.sql.Time sqlTime = CommonUtil.convert(timeMillis, java.sql.Time.class);
        assertEquals(timeMillis.longValue(), sqlTime.getTime());
    }

    @Test
    public void testConvertArrayToCollection() {
        String[] array = { "x", "y", "z" };
        List<String> list = CommonUtil.convert(array, List.class);
        assertEquals(3, list.size());
        assertEquals("x", list.get(0));
        assertEquals("y", list.get(1));
        assertEquals("z", list.get(2));

        Integer[] intArray = { 1, 2, 3 };
        Set<Integer> set = CommonUtil.convert(intArray, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testConvertCharArrayFromReader() {
        String data = "World";
        Reader reader = new StringReader(data);

        char[] result = CommonUtil.convert(reader, char[].class);
        assertArrayEquals(data.toCharArray(), result);
    }

    @Test
    public void testConvertStringFromCharSequence() {
        StringBuilder sb = new StringBuilder("Hello");
        assertEquals("Hello", CommonUtil.convert(sb, String.class));

        StringBuffer sbuf = new StringBuffer("World");
        assertEquals("World", CommonUtil.convert(sbuf, String.class));

        CharSequence cs = "Test";
        assertEquals("Test", CommonUtil.convert(cs, String.class));
    }

    @Test
    public void testConvertStringFromReader() {
        String data = "Reader Content";
        Reader reader = new StringReader(data);

        String result = CommonUtil.convert(reader, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertPrimitiveTypes() {
        assertEquals(123, CommonUtil.convert(123, int.class).intValue());
        assertEquals(456L, CommonUtil.convert(456L, long.class).longValue());
        assertEquals(78.9f, CommonUtil.convert(78.9f, float.class).floatValue(), 0.001);
        assertEquals(12.34d, CommonUtil.convert(12.34d, double.class).doubleValue(), 0.001);
        assertTrue(CommonUtil.convert(true, boolean.class));
        assertFalse(CommonUtil.convert(false, boolean.class));
        assertEquals('A', CommonUtil.convert('A', char.class).charValue());
        assertEquals((byte) 99, CommonUtil.convert((byte) 99, byte.class).byteValue());
        assertEquals((short) 999, CommonUtil.convert((short) 999, short.class).shortValue());
    }

    @Test
    public void testConvertUsingType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

        assertEquals(123, CommonUtil.convert("123", intType).intValue());

        assertEquals("456", CommonUtil.convert(456, stringType));

        assertTrue(CommonUtil.convert(1, boolType));
        assertFalse(CommonUtil.convert(0, boolType));
    }

    @Test
    public void testConvertCollectionWithType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Set> setType = TypeFactory.getType(Set.class);

        List<String> list = Arrays.asList("a", "b", "c");
        Set<String> set = CommonUtil.convert(list, setType);
        assertEquals(3, set.size());

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = CommonUtil.convert(intSet, listType);
        assertEquals(3, intList.size());
    }

    @Test
    public void testConvertWithParameterizedType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Map> mapType = TypeFactory.getType(Map.class);

        String[] array = { "x", "y", "z" };
        List<String> list = CommonUtil.convert(array, listType);
        assertEquals(3, list.size());

        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);

        Map<String, Integer> destMap = CommonUtil.convert(srcMap, mapType);
        assertEquals(2, destMap.size());
    }

    @Test
    public void testConvertAutoCloseableResources() {
        String data = "AutoCloseable test";

        class TestAutoCloseable implements AutoCloseable {
            boolean closed = false;
            String value = data;

            @Override
            public void close() {
                closed = true;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        TestAutoCloseable resource = new TestAutoCloseable();
        String result = CommonUtil.convert(resource, String.class);

        assertEquals(data, result);
        assertFalse(resource.closed);
    }

    @Test
    public void testConvert_Collections() {
        List<String> strList = Arrays.asList("1", "2", "3");
        List<String> convertedList = CommonUtil.convert(strList, List.class);
        assertEquals(strList, convertedList);

        Set<String> strSet = new HashSet<>(Arrays.asList("1", "2", "3"));
        Set<String> convertedSet = CommonUtil.convert(strSet, Set.class);
        assertEquals(strSet, convertedSet);

        String[] strArray = { "1", "2", "3" };
        Collection<String> collection = CommonUtil.convert(strArray, Collection.class);
        assertEquals(3, collection.size());
        assertTrue(collection.contains("1"));
        assertTrue(collection.contains("2"));
        assertTrue(collection.contains("3"));

        List<String> list = Arrays.asList("1", "2", "3");
        String[] array = CommonUtil.convert(list, String[].class);
        assertArrayEquals(new String[] { "1", "2", "3" }, array);
    }

    @Test
    public void testConvert_Maps() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        Map<String, Integer> convertedMap = CommonUtil.convert(map, Map.class);
        assertEquals(map, convertedMap);
    }

    @Test
    public void testConvert_Dates() {
        long timestamp = 1000000L;
        assertEquals(new java.util.Date(timestamp), CommonUtil.convert(timestamp, java.util.Date.class));
        assertEquals(new java.sql.Timestamp(timestamp), CommonUtil.convert(timestamp, java.sql.Timestamp.class));
        assertEquals(new java.sql.Date(timestamp), CommonUtil.convert(timestamp, java.sql.Date.class));
        assertEquals(new java.sql.Time(timestamp), CommonUtil.convert(timestamp, java.sql.Time.class));

        java.util.Date date = new java.util.Date(timestamp);
        assertEquals(Long.valueOf(timestamp), CommonUtil.convert(date, Long.class));
    }

    // ========== convert: CharSequence -> String ==========

    @Test
    public void testConvert_StringBuilderToString_UsesCharSequencePath() {
        // L10350: CharSequence -> String via ((CharSequence) srcObj).toString()
        StringBuilder sb = new StringBuilder("hello");
        String result = CommonUtil.convert(sb, String.class);
        assertEquals("hello", result);
    }

    @Test
    public void testConvertNullToClass() {
        assertNull(CommonUtil.convert(null, String.class));
        assertNull(CommonUtil.convert(null, Integer.class));
        assertNull(CommonUtil.convert(null, Long.class));
        assertNull(CommonUtil.convert(null, Float.class));
        assertNull(CommonUtil.convert(null, Double.class));
        assertNull(CommonUtil.convert(null, Boolean.class));
        assertNull(CommonUtil.convert(null, Object.class));
        assertNull(CommonUtil.convert(null, Date.class));
        assertNull(CommonUtil.convert(null, List.class));
        assertNull(CommonUtil.convert(null, Map.class));
    }

    @Test
    public void testConvertStringToNumbers() {
        assertEquals(123, CommonUtil.convert("123", Integer.class).intValue());
        assertEquals(123L, CommonUtil.convert("123", Long.class).longValue());
        assertEquals(123.45f, CommonUtil.convert("123.45", Float.class).floatValue(), 0.001);
        assertEquals(123.45d, CommonUtil.convert("123.45", Double.class).doubleValue(), 0.001);
        assertEquals((byte) 123, CommonUtil.convert("123", Byte.class).byteValue());
        assertEquals((short) 123, CommonUtil.convert("123", Short.class).shortValue());
        assertEquals(new BigInteger("123456789012345678901234567890"), CommonUtil.convert("123456789012345678901234567890", BigInteger.class));
        assertEquals(new BigDecimal("123.456789"), CommonUtil.convert("123.456789", BigDecimal.class));
        assertNull(CommonUtil.convert("", Integer.class));
    }

    @Test
    public void testConvertCollectionToCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Set<String> set = CommonUtil.convert(list, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = CommonUtil.convert(intSet, List.class);
        assertEquals(3, intList.size());
        assertTrue(intList.containsAll(intSet));

        List<String> emptyList = new ArrayList<>();
        Set<String> emptySet = CommonUtil.convert(emptyList, Set.class);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testConvertCollectionToArray() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CommonUtil.convert(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Integer[] intArray = CommonUtil.convert(set, Integer[].class);
        assertEquals(3, intArray.length);

        List<String> emptyList = new ArrayList<>();
        String[] emptyArray = CommonUtil.convert(emptyList, String[].class);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testConvertSingleObjectToCollection() {
        String str = "hello";
        List<String> list = CommonUtil.convert(str, List.class);
        assertEquals(1, list.size());
        assertEquals("hello", list.get(0));

        Integer num = 42;
        Set<Integer> set = CommonUtil.convert(num, Set.class);
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test
    public void testConvertSingleObjectToArray() {
        String str = "world";
        String[] array = CommonUtil.convert(str, String[].class);
        assertEquals(1, array.length);
        assertEquals("world", array[0]);

        Integer num = 99;
        Integer[] intArray = CommonUtil.convert(num, Integer[].class);
        assertEquals(1, intArray.length);
        assertEquals(99, intArray[0].intValue());
    }

    @Test
    public void testConvertMapToMap() {
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);
        srcMap.put("three", 3);

        Map<String, Integer> destMap = CommonUtil.convert(srcMap, Map.class);
        assertEquals(3, destMap.size());
        assertEquals(1, destMap.get("one").intValue());
        assertEquals(2, destMap.get("two").intValue());
        assertEquals(3, destMap.get("three").intValue());

        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> convertedEmpty = CommonUtil.convert(emptyMap, Map.class);
        assertTrue(convertedEmpty.isEmpty());
    }

    @Test
    public void testConvertByteArrayFromInputStream() {
        byte[] data = { 10, 20, 30, 40, 50 };
        InputStream is = new ByteArrayInputStream(data);

        byte[] result = CommonUtil.convert(is, byte[].class);
        assertArrayEquals(data, result);
    }

    @Test
    public void testConvertCharArrayFromInputStream() {
        String data = "['T','e','s','t']";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        char[] result = CommonUtil.convert(is, char[].class);
        assertEquals("Test", String.valueOf(result));
    }

    @Test
    public void testConvertStringFromInputStream() {
        String data = "InputStream Content";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        String result = CommonUtil.convert(is, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertInputStreamReadFailureRetainsFailedCloseAsSuppressed() {
        final InputStream input = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        final UncheckedIOException exception = assertThrows(UncheckedIOException.class, () -> CommonUtil.convert(input, byte[].class));

        assertEquals("read failed", exception.getCause().getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertTrue(exception.getSuppressed()[0] instanceof UncheckedIOException);
        assertEquals("close failed", exception.getSuppressed()[0].getCause().getMessage());
    }

    @Test
    public void testConvertReaderToStringFailureRetainsFailedCloseAsSuppressed() {
        final Reader reader = new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("close failed");
            }
        };

        final UncheckedIOException exception = assertThrows(UncheckedIOException.class, () -> CommonUtil.convert(reader, String.class));

        assertEquals("read failed", exception.getCause().getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertTrue(exception.getSuppressed()[0] instanceof UncheckedIOException);
        assertEquals("close failed", exception.getSuppressed()[0].getCause().getMessage());
    }

    @Test
    public void testConvertSameType() {
        String str = "test";
        assertSame(str, CommonUtil.convert(str, String.class));

        Integer num = 42;
        assertSame(num, CommonUtil.convert(num, Integer.class));

        List<String> list = new ArrayList<>();
        assertSame(list, CommonUtil.convert(list, ArrayList.class));
    }

    @Test
    public void testConvertNullToType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<List> listType = TypeFactory.getType(List.class);

        assertNull(CommonUtil.convert(null, stringType));
        assertNull(CommonUtil.convert(null, intType));
        assertNull(CommonUtil.convert(null, listType));
    }

    @Test
    public void testConvert_Basic() {
        assertEquals(0, CommonUtil.convert(null, int.class));
        assertEquals(null, CommonUtil.convert(null, Integer.class));
        assertEquals(null, CommonUtil.convert(null, String.class));

        assertEquals(Integer.valueOf(123), CommonUtil.convert("123", Integer.class));
        assertEquals(Long.valueOf(123L), CommonUtil.convert("123", Long.class));
        assertEquals(Double.valueOf(3.14), CommonUtil.convert("3.14", Double.class));
        assertEquals(Boolean.TRUE, CommonUtil.convert("true", Boolean.class));

        assertEquals(Integer.valueOf(123), CommonUtil.convert(123L, Integer.class));
        assertEquals(Long.valueOf(123L), CommonUtil.convert(123, Long.class));
        assertEquals(Float.valueOf(3.14f), CommonUtil.convert(3.14d, Float.class));
        assertEquals(Double.valueOf(3.14d), CommonUtil.convert(3.14f, Double.class));

        assertEquals(true, CommonUtil.convert(1, boolean.class));
        assertEquals(false, CommonUtil.convert(0, boolean.class));
        assertEquals(true, CommonUtil.convert(5L, Boolean.class));

        assertEquals(Character.valueOf('A'), CommonUtil.convert(65, Character.class));
        assertEquals(Integer.valueOf(65), CommonUtil.convert('A', Integer.class));
    }

    @Test
    public void testConvert_WithType() {
        Type<Integer> intType = CommonUtil.typeOf(int.class);
        assertEquals(Integer.valueOf(123), CommonUtil.convert("123", intType));
        assertEquals(0, CommonUtil.convert(null, intType));

        Type<List> listType = CommonUtil.typeOf(List.class);
        String[] array = { "1", "2", "3" };
        List<String> list = CommonUtil.convert(array, listType);
        assertEquals(3, list.size());
    }

    @Test
    public void testConvertStringToNumbersWithInvalidFormat() {
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert("abc", Integer.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert("12.34.56", Double.class));
    }

    @Test
    public void testConvertStringToCharacter() {
        assertEquals('A', CommonUtil.convert("A", Character.class).charValue());
        assertEquals('1', CommonUtil.convert("1", Character.class).charValue());
        assertThrows(RuntimeException.class, () -> CommonUtil.convert("AB", Character.class));
        assertNull(CommonUtil.convert("", Character.class));
    }

    @Test
    public void testConvertByteArrayFromBlob() throws SQLException {
        Blob blob = new Blob() {
            private boolean freed = false;

            @Override
            public long length() throws SQLException {
                return 5;
            }

            @Override
            public byte[] getBytes(long pos, int length) throws SQLException {
                if (freed) {
                    throw new SQLException("Blob already freed");
                }
                return new byte[] { 1, 2, 3, 4, 5 };
            }

            @Override
            public InputStream getBinaryStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(byte[] pattern, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(Blob pattern, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setBytes(long pos, byte[] bytes) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream setBinaryStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void free() throws SQLException {
                freed = true;
            }

            @Override
            public InputStream getBinaryStream(long pos, long length) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };

        byte[] result = CommonUtil.convert(blob, byte[].class);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConvertBlobSizeFailureRetainsFailedCleanupAsSuppressed() {
        final Blob blob = (Blob) Proxy.newProxyInstance(CommonUtilTest.class.getClassLoader(), new Class<?>[] { Blob.class }, (proxy, method, args) -> {
            if (method.getName().equals("length")) {
                return (long) Integer.MAX_VALUE + 1;
            } else if (method.getName().equals("free")) {
                throw new SQLException("free failed");
            }

            throw new UnsupportedOperationException(method.getName());
        });

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(blob, byte[].class));

        assertTrue(exception.getMessage().contains("exceeds maximum array size"));
        assertEquals(1, exception.getSuppressed().length);
        assertTrue(exception.getSuppressed()[0] instanceof UncheckedSQLException);
        assertEquals("free failed", exception.getSuppressed()[0].getCause().getMessage());
    }

    @Test
    public void testConvertBlobDoesNotSelfSuppressSharedConversionAndCleanupFailure() {
        final IllegalStateException sharedFailure = new IllegalStateException("shared failure");
        final Blob blob = (Blob) Proxy.newProxyInstance(CommonUtilTest.class.getClassLoader(), new Class<?>[] { Blob.class }, (proxy, method, args) -> {
            if (method.getName().equals("length")) {
                return 1L;
            } else if (method.getName().equals("getBytes") || method.getName().equals("free")) {
                throw sharedFailure;
            }

            throw new UnsupportedOperationException(method.getName());
        });

        final IllegalStateException exception = assertThrows(IllegalStateException.class, () -> CommonUtil.convert(blob, byte[].class));

        assertSame(sharedFailure, exception);
        assertEquals(0, exception.getSuppressed().length);
    }

    @Test
    public void testConvertClobSizeFailureRetainsFailedCleanupAsSuppressed() {
        final Clob clob = (Clob) Proxy.newProxyInstance(CommonUtilTest.class.getClassLoader(), new Class<?>[] { Clob.class }, (proxy, method, args) -> {
            if (method.getName().equals("length")) {
                return (long) Integer.MAX_VALUE + 1;
            } else if (method.getName().equals("free")) {
                throw new SQLException("free failed");
            }

            throw new UnsupportedOperationException(method.getName());
        });

        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(clob, char[].class));

        assertTrue(exception.getMessage().contains("exceeds maximum array size"));
        assertEquals(1, exception.getSuppressed().length);
        assertTrue(exception.getSuppressed()[0] instanceof UncheckedSQLException);
        assertEquals("free failed", exception.getSuppressed()[0].getCause().getMessage());
    }

    @Test
    public void testConvertInputStreamFromByteArray() {
        byte[] data = { 1, 2, 3, 4, 5 };
        InputStream is = CommonUtil.convert(data, InputStream.class);

        assertNotNull(is);
        assertTrue(is instanceof ByteArrayInputStream);

        byte[] readData = new byte[5];
        try {
            is.read(readData);
            assertArrayEquals(data, readData);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testConvertReaderFromCharSequence() {
        String data = "Test String";
        Reader reader = CommonUtil.convert(data, Reader.class);

        assertNotNull(reader);
        assertTrue(reader instanceof StringReader);

        char[] buffer = new char[data.length()];
        try {
            reader.read(buffer);
            assertArrayEquals(data.toCharArray(), buffer);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testConvertWithSQLException() {
        Blob blob = new Blob() {
            @Override
            public long length() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public byte[] getBytes(long pos, int length) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public InputStream getBinaryStream() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public long position(byte[] pattern, long start) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public long position(Blob pattern, long start) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public int setBytes(long pos, byte[] bytes) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public OutputStream setBinaryStream(long pos) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public void free() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public InputStream getBinaryStream(long pos, long length) throws SQLException {
                throw new SQLException("Test exception");
            }
        };

        assertThrows(UncheckedSQLException.class, () -> CommonUtil.convert(blob, byte[].class));
    }

    @Test
    public void testConvertEdgeCases() {

        String bigNum = "9223372036854775807";
        assertEquals(Long.MAX_VALUE, CommonUtil.convert(bigNum, Long.class).longValue());

        assertThrows(ArithmeticException.class, () -> CommonUtil.convert(Long.MAX_VALUE, Integer.class).intValue());
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert(1000, Byte.class).byteValue());

        // Unified policy: an out-of-range numeric STRING overflows -> ArithmeticException (not NumberFormatException),
        // matching the Number path above. A malformed string still throws NumberFormatException (see 9209/9210).
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert("128", Byte.class));
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert("40000", Short.class));
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert("9999999999", Integer.class));
        assertThrows(ArithmeticException.class, () -> CommonUtil.convert("9223372036854775808", Long.class));

        assertEquals((Float) Float.POSITIVE_INFINITY, CommonUtil.convert("Infinity", Float.class));
        assertEquals((Double) Double.NEGATIVE_INFINITY, CommonUtil.convert("-Infinity", Double.class));
        assertTrue(Double.isNaN(CommonUtil.convert("NaN", Double.class).doubleValue()));
    }

    @Test
    public void testConvertUnsupportedConversions() {

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        assertThrows(RuntimeException.class, () -> CommonUtil.convert(map, Integer.class));
        assertFalse(CommonUtil.convert(map, Boolean.class));

        List<Map<String, Object>> complexList = new ArrayList<>();
        complexList.add(new HashMap<>());

        List<Map<String, Object>> convertedList = CommonUtil.convert(complexList, List.class);
        assertNotNull(convertedList);
        assertEquals(1, convertedList.size());
    }

    @Test
    public void testConvert_ClobToString_uncovered() {
        StubClob clob = new StubClob("hello world");
        String result = CommonUtil.convert(clob, String.class);
        assertEquals("hello world", result);
        assertTrue(clob.freed, "clob should be freed after conversion");
    }

    @Test
    public void testConvert_ClobToCharArray_uncovered() {
        StubClob clob = new StubClob("abc");
        char[] result = CommonUtil.convert(clob, char[].class);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertTrue(clob.freed, "clob should be freed after conversion");
    }

    @Test
    public void testConvert_MapToMap_CopyPath_uncovered() {
        Map<String, Integer> src = new LinkedHashMap<>();
        src.put("a", 1);
        src.put("b", 2);
        @SuppressWarnings("unchecked")
        Map<String, Integer> result = CommonUtil.convert(src, (Class<Map<String, Integer>>) (Class<?>) TreeMap.class);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1, (int) result.get("a"));
        assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testConvert_CollectionToCollection_CopyPath_uncovered() {
        List<Integer> src = Arrays.asList(1, 2, 3, 2);
        @SuppressWarnings("unchecked")
        Set<Integer> result = CommonUtil.convert(src, (Class<Set<Integer>>) (Class<?>) LinkedHashSet.class);
        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result instanceof LinkedHashSet);
    }

    @Test
    public void testConvert_GenericCollectionMapAndArrayTargets() {
        final Type<List> listType = TypeFactory.getType(List.class);
        final List<String> list = CommonUtil.convert(new String[] { "a", "b" }, listType);
        assertEquals(Arrays.asList("a", "b"), list);

        final Map<String, Integer> sourceMap = new LinkedHashMap<>();
        sourceMap.put("one", 1);
        sourceMap.put("two", 2);
        final Type<Map> mapType = TypeFactory.getType(Map.class);
        final Map<String, Integer> convertedMap = CommonUtil.convert(sourceMap, mapType);
        assertEquals(sourceMap, convertedMap);

        final String[] array = CommonUtil.convert(Arrays.asList("x", "y"), String[].class);
        assertArrayEquals(new String[] { "x", "y" }, array);
    }

    // ---------------------------------------------------------------------------------------------------------
    // Fix pass 2026-09-08 (G01)
    // ---------------------------------------------------------------------------------------------------------

    // G01-24: convert applied its own documented rule ("a Number is true iff it is non-zero") only when the
    // SOURCE TYPE was classified as a number. A Number whose handler is not a number type - AtomicInteger and
    // AtomicLong are the plain-JDK cases - fell through to Type<Boolean>.valueOf and its separately documented
    // "> 0" rule, so this one method answered true for -1 and false for an AtomicInteger holding -1.
    @Test
    public void fixG01_convertReadsEveryNumberAsNonZeroTruthiness() {
        assertTrue(CommonUtil.convert(-1, Boolean.class));
        assertTrue(CommonUtil.convert(new java.util.concurrent.atomic.AtomicInteger(-1), Boolean.class));
        assertTrue(CommonUtil.convert(new java.util.concurrent.atomic.AtomicLong(-7L), Boolean.class));
        assertTrue(CommonUtil.convert(new java.util.concurrent.atomic.AtomicInteger(1), Boolean.class));
        assertFalse(CommonUtil.convert(new java.util.concurrent.atomic.AtomicInteger(0), Boolean.class));
        assertFalse(CommonUtil.convert(new java.util.concurrent.atomic.AtomicLong(0L), Boolean.class));

        final Type<Boolean> primitiveBoolean = TypeFactory.getType(boolean.class);
        assertTrue(CommonUtil.convert(new java.util.concurrent.atomic.AtomicInteger(-1), primitiveBoolean));
        assertFalse(CommonUtil.convert(new java.util.concurrent.atomic.AtomicInteger(0), primitiveBoolean));

        // the Type<Boolean> handlers keep their own, separately documented "> 0" rule (JDBC and JSON/XML reads)
        final Type<Boolean> boxedBoolean = TypeFactory.getType(Boolean.class);
        assertFalse(boxedBoolean.valueOf((Object) Integer.valueOf(-1)));
        assertFalse(boxedBoolean.valueOf((Object) new java.util.concurrent.atomic.AtomicInteger(-1)));
    }

    /** A {@code Reader} that records whether it was closed, and can report whether anything is left to read. */
    private static final class ClosingProbeReader extends StringReader {
        private boolean closed = false;

        ClosingProbeReader(final String s) {
            super(s);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        /** {@code true} if nothing was consumed from this reader. Must be called before {@link #close()}. */
        boolean isUnread() throws IOException {
            return read() >= 0;
        }
    }

    /** An {@code InputStream} that records whether it was closed. */
    private static final class ClosingProbeStream extends ByteArrayInputStream {
        private boolean closed = false;

        ClosingProbeStream(final byte[] b) {
            super(b);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    /**
     * A stream source drained by a numeric conversion must be closed, exactly as for a {@code String} target.
     *
     * <p>The number branch returned straight out of {@code targetType.valueOf(srcObj)}, jumping over the closing
     * block at the end of {@code convert}, so the source was read to EOF and then left open.</p>
     */
    @Test
    public void testConvert_ClosesReaderForNumberTarget() {
        final ClosingProbeReader forString = new ClosingProbeReader("42");
        assertEquals("42", CommonUtil.convert(forString, String.class));
        assertTrue(forString.closed);

        for (final Class<?> target : new Class<?>[] { Integer.class, Long.class, Short.class, Double.class, Float.class }) {
            final ClosingProbeReader reader = new ClosingProbeReader("42");
            assertEquals(42, ((Number) CommonUtil.convert(reader, target)).intValue());
            assertTrue(reader.closed, "source drained by a " + target.getSimpleName() + " conversion must be closed");
        }
    }

    @Test
    public void testConvert_ClosesInputStreamForNumberTarget() {
        for (final Class<?> target : new Class<?>[] { Integer.class, Long.class, Double.class }) {
            final ClosingProbeStream stream = new ClosingProbeStream("42".getBytes());
            assertEquals(42, ((Number) CommonUtil.convert(stream, target)).intValue());
            assertTrue(stream.closed, "source drained by a " + target.getSimpleName() + " conversion must be closed");
        }
    }

    /** Same contract for a {@code char}/{@code Character} target, which also drains the source. */
    @Test
    public void testConvert_ClosesReaderForCharacterTarget() {
        final ClosingProbeReader reader = new ClosingProbeReader("x");
        assertEquals(Character.valueOf('x'), CommonUtil.convert(reader, Character.class));
        assertTrue(reader.closed);

        final ClosingProbeStream stream = new ClosingProbeStream("x".getBytes());
        assertEquals(Character.valueOf('x'), CommonUtil.convert(stream, Character.class));
        assertTrue(stream.closed);
    }

    /**
     * A conversion failure must still close a drained source, and must surface as the conversion error rather
     * than anything thrown while closing.
     */
    @Test
    public void testConvert_ClosesReaderForNumberTarget_ConversionFails() {
        final ClosingProbeReader reader = new ClosingProbeReader("not-a-number");

        assertThrows(RuntimeException.class, () -> CommonUtil.convert(reader, Integer.class));
        assertTrue(reader.closed);
    }

    /**
     * A {@code Boolean} target must leave the source OPEN, because it never reads it.
     *
     * <p>This pins a deliberate asymmetry, so that making the branches "consistent" cannot quietly break the
     * documented contract: {@code convert} releases a source it <i>consumes</i>, and explicitly states that
     * "conversion paths that do not consume the source ... do not close it". {@code Type<Boolean>.valueOf} ignores
     * a stream source entirely - the reader is still positioned at its first character afterwards - so closing it
     * here would be closing a source {@code convert} never consumed.</p>
     */
    @Test
    public void testConvert_DoesNotCloseReaderForBooleanTarget() throws IOException {
        final ClosingProbeReader reader = new ClosingProbeReader("true");

        CommonUtil.convert(reader, Boolean.class);

        assertFalse(reader.closed, "a Boolean conversion does not consume the source, so it must not close it");
        assertTrue(reader.isUnread(), "nothing should have been read from the source");

        final ClosingProbeStream stream = new ClosingProbeStream("true".getBytes());
        CommonUtil.convert(stream, Boolean.class);
        assertFalse(stream.closed);
    }
}
