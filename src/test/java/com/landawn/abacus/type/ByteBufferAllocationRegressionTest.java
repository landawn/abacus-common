package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class ByteBufferAllocationRegressionTest extends TestBase {

    @Test
    void supportedHeapAndDirectTargetsPreserveOwnershipAndPosition() {
        final byte[] bytes = { 1, 2, 3 };

        for (final Class<? extends ByteBuffer> target : List.of(ByteBuffer.class, ByteBuffer.allocate(0).getClass(), ByteBuffer.allocateDirect(0).getClass(),
                MappedByteBuffer.class)) {
            final ByteBufferType type = new ByteBufferType(target);
            final ByteBuffer result = type.valueOf((Object) bytes);
            assertTrue(target.isInstance(result));
            assertEquals(bytes.length, result.position());
            assertEquals(bytes.length, result.limit());
            assertArrayEquals(bytes, ByteBufferType.byteArrayOf(result));

            if (result.isDirect()) {
                assertFalse(result.hasArray());
            } else {
                assertSame(bytes, result.array());
            }

            final ByteBuffer empty = type.valueOf("");
            assertTrue(target.isInstance(empty));
            assertEquals(0, empty.position());
            assertEquals(0, empty.limit());
            assertNull(type.valueOf((String) null));
        }
    }

    @Test
    void unsupportedReadOnlyTargetsKeepValidationAndFailureOrder() {
        for (final Class<? extends ByteBuffer> target : List.of(ByteBuffer.allocate(0).asReadOnlyBuffer().getClass(),
                ByteBuffer.allocateDirect(0).asReadOnlyBuffer().getClass())) {
            final Type<?> type = Type.of(target);
            final String expected = "Content construction is not supported for byte buffer class: " + target.getName();
            assertEquals(expected, assertThrows(UnsupportedOperationException.class, () -> type.valueOf(new byte[1024])).getMessage());
            assertEquals(expected, assertThrows(UnsupportedOperationException.class, () -> type.valueOf("AQID")).getMessage());
            assertEquals(expected, assertThrows(UnsupportedOperationException.class, () -> type.valueOf("")).getMessage());
            assertThrows(IllegalArgumentException.class, () -> type.valueOf("###"));
            assertNull(type.valueOf((String) null));
        }
    }

    @Test
    void rejectedTargetsDoNotAllocateTheirPayloadAsADirectBuffer() {
        final BufferPoolMXBean directPool = ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class)
                .stream()
                .filter(pool -> "direct".equals(pool.getName()))
                .findFirst()
                .orElseThrow();
        final Type<?> type = Type.of(ByteBuffer.allocateDirect(0).asReadOnlyBuffer().getClass());
        final byte[] payload = new byte[1 << 20];

        // Initializes the direct-class probe (a zero-capacity buffer) outside the measured window.
        assertThrows(UnsupportedOperationException.class, () -> type.valueOf(payload));

        final long capacityBefore = directPool.getTotalCapacity();

        for (int i = 0; i < 4; i++) {
            assertThrows(UnsupportedOperationException.class, () -> type.valueOf(payload));
        }

        // Rejection used to come after a payload-sized direct allocation, which stayed reserved until a GC ran its cleaner.
        final long growth = directPool.getTotalCapacity() - capacityBefore;
        assertTrue(growth < payload.length, "rejected conversions reserved " + growth + " bytes of direct memory");
    }
}
