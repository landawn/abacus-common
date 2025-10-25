package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ByteSupplier2025Test extends TestBase {

    @Test
    public void testGetAsByte() {
        ByteSupplier supplier = () -> (byte) 42;

        assertEquals((byte) 42, supplier.getAsByte());
    }

    @Test
    public void testGetAsByteWithLambda() {
        ByteSupplier supplier = () -> (byte) -10;

        assertEquals((byte) -10, supplier.getAsByte());
        assertEquals((byte) -10, supplier.getAsByte());
    }

    @Test
    public void testGetAsByteWithAnonymousClass() {
        ByteSupplier supplier = new ByteSupplier() {
            @Override
            public byte getAsByte() {
                return (byte) 100;
            }
        };

        assertEquals((byte) 100, supplier.getAsByte());
    }

    @Test
    public void testZero() {
        assertEquals((byte) 0, ByteSupplier.ZERO.getAsByte());
        assertEquals((byte) 0, ByteSupplier.ZERO.getAsByte());
    }

    @Test
    public void testRandom() {
        byte first = ByteSupplier.RANDOM.getAsByte();
        byte second = ByteSupplier.RANDOM.getAsByte();

        assertTrue(first >= Byte.MIN_VALUE && first <= Byte.MAX_VALUE);
        assertTrue(second >= Byte.MIN_VALUE && second <= Byte.MAX_VALUE);
    }

    @Test
    public void testRandomProducesDifferentValues() {
        boolean foundDifferent = false;
        byte first = ByteSupplier.RANDOM.getAsByte();

        for (int i = 0; i < 100; i++) {
            if (ByteSupplier.RANDOM.getAsByte() != first) {
                foundDifferent = true;
                break;
            }
        }

        assertTrue(foundDifferent);
    }

    @Test
    public void testStatefulSupplier() {
        final byte[] counter = { 0 };
        ByteSupplier supplier = () -> counter[0]++;

        assertEquals((byte) 0, supplier.getAsByte());
        assertEquals((byte) 1, supplier.getAsByte());
        assertEquals((byte) 2, supplier.getAsByte());
    }

    @Test
    public void testBoundaryValues() {
        ByteSupplier maxSupplier = () -> Byte.MAX_VALUE;
        ByteSupplier minSupplier = () -> Byte.MIN_VALUE;

        assertEquals(Byte.MAX_VALUE, maxSupplier.getAsByte());
        assertEquals(Byte.MIN_VALUE, minSupplier.getAsByte());
    }

    @Test
    public void testMethodReference() {
        TestObject obj = new TestObject();
        ByteSupplier supplier = obj::getValue;

        assertEquals((byte) 25, supplier.getAsByte());
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(ByteSupplier.class.getAnnotation(FunctionalInterface.class));
    }

    private static class TestObject {
        byte getValue() {
            return (byte) 25;
        }
    }
}
