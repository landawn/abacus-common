package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.nio.ByteBuffer;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;

public class ByteBufferTypeTest extends TestBase {

    private final ByteBufferType type = new ByteBufferType();

    @Test
    public void testClazz() {
        Class<ByteBuffer> result = type.javaType();
        assertEquals(ByteBuffer.class, result);
    }

    @Test
    public void testIsByteBuffer() {
        boolean result = type.isByteBuffer();
        Assertions.assertTrue(result);
    }

    @Test
    public void testComplexData() {
        ByteBuffer buffer = ByteBuffer.allocate(256);
        for (int i = 0; i < 256; i++) {
            buffer.put((byte) i);
        }

        String base64 = type.stringOf(buffer);
        ByteBuffer restored = type.valueOf(base64);

        assertEquals(256, restored.position());
        restored.position(0);
        for (int i = 0; i < 256; i++) {
            assertEquals((byte) i, restored.get());
        }
    }

    @Test
    public void testStringOf_EmptyBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(0);
        String result = type.stringOf(buffer);
        Assertions.assertNotNull(result);
        assertEquals("", result);
    }

    @Test
    public void testStringOf_BufferWithData() {
        ByteBuffer buffer = ByteBuffer.allocate(3);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);

        String result = type.stringOf(buffer);
        Assertions.assertNotNull(result);
        assertEquals("AQID", result);
    }

    @Test
    public void testRoundTrip() {
        ByteBuffer original = ByteBuffer.allocate(4);
        original.put((byte) 10);
        original.put((byte) 20);
        original.put((byte) 30);
        original.put((byte) 40);

        String base64 = type.stringOf(original);
        Assertions.assertNotNull(base64);

        ByteBuffer restored = type.valueOf(base64);
        Assertions.assertNotNull(restored);

        assertEquals(original.position(), restored.position());
        assertEquals(original.limit(), restored.limit());

        original.position(0);
        restored.position(0);
        while (original.hasRemaining()) {
            assertEquals(original.get(), restored.get());
        }
    }

    @Test
    public void testValueOf_Null() {
        ByteBuffer result = type.valueOf((String) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOf_EmptyString() {
        ByteBuffer result = type.valueOf("");
        Assertions.assertNotNull(result);
        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, result.capacity());
    }

    @Test
    public void testValueOf_ValidBase64() {
        String base64 = "AQID";
        ByteBuffer result = type.valueOf(base64);

        Assertions.assertNotNull(result);
        assertEquals(3, result.position());
        assertEquals(3, result.limit());
        assertEquals(3, result.capacity());

        result.position(0);
        assertEquals((byte) 1, result.get());
        assertEquals((byte) 2, result.get());
        assertEquals((byte) 3, result.get());
    }

    @Test
    public void testValueOf_ByteArray() {
        byte[] bytes = new byte[] { (byte) -1, (byte) 0, (byte) 127 };
        ByteBuffer result = ByteBufferType.valueOf(bytes);

        Assertions.assertNotNull(result);
        assertEquals(3, result.position());
        assertEquals(3, result.limit());
        assertEquals(3, result.capacity());

        result.position(0);
        assertEquals((byte) -1, result.get());
        assertEquals((byte) 0, result.get());
        assertEquals((byte) 127, result.get());
    }

    @Test
    public void testValueOf_EmptyByteArray() {
        byte[] bytes = new byte[0];
        ByteBuffer result = ByteBufferType.valueOf(bytes);

        Assertions.assertNotNull(result);
        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, result.capacity());
    }

    @Test
    public void testByteArrayOf() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put((byte) 10);
        buffer.put((byte) 20);
        buffer.put((byte) 30);
        buffer.put((byte) 40);

        byte[] result = ByteBufferType.byteArrayOf(buffer);

        Assertions.assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals((byte) 10, result[0]);
        assertEquals((byte) 20, result[1]);
        assertEquals((byte) 30, result[2]);
        assertEquals((byte) 40, result[3]);

        assertEquals(4, buffer.position());
    }

    @Test
    public void testByteArrayOf_PartialBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(5);
        buffer.put((byte) 1);
        buffer.put((byte) 2);
        buffer.put((byte) 3);

        byte[] result = ByteBufferType.byteArrayOf(buffer);

        Assertions.assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals((byte) 1, result[0]);
        assertEquals((byte) 2, result[1]);
        assertEquals((byte) 3, result[2]);

        assertEquals(3, buffer.position());
    }

    @Test
    public void testStringOf_PreservesMarkPositionAndLimit() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put((byte) 1).put((byte) 2);
        buffer.mark();
        buffer.put((byte) 3);
        buffer.limit(6);

        assertEquals("AQID", type.stringOf(buffer));
        assertEquals(3, buffer.position());
        assertEquals(6, buffer.limit());

        buffer.reset();
        assertEquals(2, buffer.position());
    }

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    @Test
    public void test_javaType_reflectsConcreteSubclass() {
        // regression: the Class-arg constructor dropped the concrete subclass, so a handler
        // named "MappedByteBuffer" reported javaType() == ByteBuffer.class, unlike the
        // InputStreamType/ReaderType siblings which return the requested class.
        assertEquals(java.nio.MappedByteBuffer.class, new ByteBufferType(java.nio.MappedByteBuffer.class).javaType());
        assertEquals(ByteBuffer.class, new ByteBufferType().javaType());
    }

    // ---- review fixes 2026-09-06: T3-04 valueOf(Object) with a byte[] ----

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayWrapsRawBytes() {
        final byte[] bytes = { 1, 2, 3 };
        final ByteBuffer result = type.valueOf((Object) bytes);

        assertEquals(3, result.position());
        assertEquals(3, result.limit());
        assertEquals(3, result.capacity());
        Assertions.assertArrayEquals(bytes, ByteBufferType.byteArrayOf(result));
        // same semantics as the static valueOf(byte[]): the array is wrapped, not copied
        Assertions.assertSame(bytes, result.array());
        assertEquals("AQID", type.stringOf(result));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectEmptyByteArrayGivesEmptyBuffer() {
        final ByteBuffer result = type.valueOf((Object) new byte[0]);

        assertEquals(0, result.position());
        assertEquals(0, result.limit());
        assertEquals(0, ByteBufferType.byteArrayOf(result).length);
    }

    @Test
    public void reviewFixes20260906_valueOfObjectOtherInputsUnchanged() {
        Assertions.assertNull(type.valueOf((Object) null));

        final ByteBuffer fromString = type.valueOf((Object) "AQID");
        assertEquals(3, fromString.position());
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, ByteBufferType.byteArrayOf(fromString));

        final ByteBuffer original = ByteBuffer.allocate(8);
        original.put((byte) 9).put((byte) 8);
        final ByteBuffer fromBuffer = type.valueOf((Object) original);
        Assertions.assertNotSame(original, fromBuffer);
        Assertions.assertArrayEquals(new byte[] { 9, 8 }, ByteBufferType.byteArrayOf(fromBuffer));
        assertEquals(2, fromBuffer.position());
        assertEquals(2, original.position()); // untouched
        assertEquals(8, original.limit());

        // invalid text still fails as Base64 (the list text is exactly what a byte[] used to become)
        Assertions.assertThrows(IllegalArgumentException.class, () -> type.valueOf((Object) "[1, 2, 3]"));
    }

    @Test
    public void reviewFixes20260906_convertByteArrayToByteBuffer() {
        final ByteBuffer result = N.convert(new byte[] { 1, 2, 3 }, ByteBuffer.class);

        assertEquals(3, result.position());
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, ByteBufferType.byteArrayOf(result));
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 },
                ByteBufferType.byteArrayOf(Type.of(ByteBuffer.class).valueOf((Object) new byte[] { 1, 2, 3 })));
    }

    @Test
    public void reviewFixes20260906_valueOfObjectByteArrayOnSubclassHandlerMatchesStringPath() {
        final ByteBufferType mapped = new ByteBufferType(java.nio.MappedByteBuffer.class);

        // both paths agree, and both now build an instance of the DECLARED subclass (they used to hand
        // back a plain heap buffer, which javaType() == MappedByteBuffer.class never admitted)
        assertEquals(mapped.valueOf("AQID").getClass(), mapped.valueOf((Object) new byte[] { 1, 2, 3 }).getClass());
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, ByteBufferType.byteArrayOf(mapped.valueOf((Object) new byte[] { 1, 2, 3 })));
        Assertions.assertInstanceOf(java.nio.MappedByteBuffer.class, mapped.valueOf("AQID"));
        Assertions.assertInstanceOf(java.nio.MappedByteBuffer.class, mapped.valueOf((Object) new byte[] { 1, 2, 3 }));
    }

    // ---- F77/F87 review fixes 2026-09-08: valueOf must produce an instance of javaType() ----

    @Test
    public void reviewFixes20260908_valueOfAlwaysProducesAnInstanceOfJavaType() {
        // A handler bound to a ByteBuffer subclass used to advertise that class from javaType() while every
        // valueOf ended at ByteBuffer.wrap(..), a HeapByteBuffer that is not an instance of it.
        for (final ByteBufferType handler : new ByteBufferType[] { new ByteBufferType(), new ByteBufferType(ByteBuffer.class),
                new ByteBufferType(java.nio.MappedByteBuffer.class) }) {
            final ByteBuffer fromString = handler.valueOf("AQID");
            final ByteBuffer fromBytes = handler.valueOf((Object) new byte[] { 1, 2, 3 });
            final ByteBuffer empty = handler.valueOf("");

            for (final ByteBuffer buffer : new ByteBuffer[] { fromString, fromBytes, empty }) {
                Assertions.assertTrue(handler.javaType().isInstance(buffer), handler.javaType() + " vs " + buffer.getClass());
            }

            // the position-at-end convention and the Base64 round trip are unchanged for every handler
            assertEquals(3, fromString.position());
            assertEquals(3, fromBytes.position());
            assertEquals(0, empty.position());
            Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, ByteBufferType.byteArrayOf(fromString));
            Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, ByteBufferType.byteArrayOf(fromBytes));
            assertEquals("AQID", handler.stringOf(fromBytes));
            Assertions.assertNull(handler.valueOf((String) null));
        }

        // the plain-ByteBuffer handler still shares the caller's array (the static valueOf(byte[]) contract)
        final byte[] bytes = { 1, 2, 3 };
        Assertions.assertSame(bytes, type.valueOf((Object) bytes).array());
    }

    @Test
    public void reviewFixes20260908_bufferClassThatCannotBeConstructedIsRejected() {
        // Read-only buffer classes are satisfied by neither a heap nor a direct allocation; the handler must
        // say so rather than return something javaType() does not accept.
        final Class<? extends ByteBuffer> readOnlyHeap = com.landawn.abacus.util.ClassUtil.forName("java.nio.HeapByteBufferR").asSubclass(ByteBuffer.class);
        final ByteBufferType readOnly = new ByteBufferType(readOnlyHeap);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> readOnly.valueOf("AQID"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> readOnly.valueOf((Object) new byte[] { 1, 2, 3 }));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> readOnly.valueOf(""));
        // a null input never needs a buffer at all
        Assertions.assertNull(readOnly.valueOf((String) null));
    }

}
