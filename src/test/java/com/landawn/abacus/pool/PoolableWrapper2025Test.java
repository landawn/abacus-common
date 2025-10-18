package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.Immutable;

@Tag("2025")
public class PoolableWrapper2025Test extends TestBase {

    @Test
    public void testConstructorWithDefaultValues() {
        String value = "test-value";
        PoolableWrapper<String> wrapper = new PoolableWrapper<>(value);

        assertNotNull(wrapper);
        assertEquals(value, wrapper.value());
        assertNotNull(wrapper.activityPrint());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getLiveTime());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithCustomValues() {
        Integer value = 42;
        long liveTime = 10000;
        long maxIdleTime = 5000;

        PoolableWrapper<Integer> wrapper = new PoolableWrapper<>(value, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(value, wrapper.value());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithNullValue() {
        PoolableWrapper<Object> wrapper = new PoolableWrapper<>(null);

        assertNotNull(wrapper);
        assertNull(wrapper.value());
        assertNotNull(wrapper.activityPrint());
    }

    @Test
    public void testConstructorWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> new PoolableWrapper<>("test", 0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new PoolableWrapper<>("test", -1, 5000));
    }

    @Test
    public void testConstructorWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> new PoolableWrapper<>("test", 5000, 0));
        assertThrows(IllegalArgumentException.class, () -> new PoolableWrapper<>("test", 5000, -1));
    }

    @Test
    public void testOfFactoryMethodWithDefaultValues() {
        String value = "factory-test";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(value);

        assertNotNull(wrapper);
        assertEquals(value, wrapper.value());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getLiveTime());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testOfFactoryMethodWithCustomValues() {
        Double value = 3.14;
        long liveTime = 15000;
        long maxIdleTime = 7500;

        PoolableWrapper<Double> wrapper = PoolableWrapper.of(value, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(value, wrapper.value());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testOfWithNullValue() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of(null);

        assertNotNull(wrapper);
        assertNull(wrapper.value());
    }

    @Test
    public void testValue() {
        String expectedValue = "wrapped-value";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(expectedValue);

        assertEquals(expectedValue, wrapper.value());
    }

    @Test
    public void testValueWithNullWrappedObject() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of(null);

        assertNull(wrapper.value());
    }

    @Test
    public void testValueWithDifferentTypes() {
        PoolableWrapper<Integer> intWrapper = PoolableWrapper.of(123);
        assertEquals(Integer.valueOf(123), intWrapper.value());

        PoolableWrapper<String> stringWrapper = PoolableWrapper.of("hello");
        assertEquals("hello", stringWrapper.value());

        PoolableWrapper<Boolean> boolWrapper = PoolableWrapper.of(true);
        assertEquals(Boolean.TRUE, boolWrapper.value());
    }

    @Test
    public void testDestroy() {
        String value = "test";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(value);

        // destroy should be a no-op
        wrapper.destroy(Caller.CLOSE);

        // value should still be accessible
        assertEquals(value, wrapper.value());
    }

    @Test
    public void testDestroyWithDifferentCallers() {
        for (Caller caller : Caller.values()) {
            PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

            wrapper.destroy(caller);

            // Value should still be accessible after destroy
            assertNotNull(wrapper.value());
            assertEquals("test", wrapper.value());
        }
    }

    @Test
    public void testDestroyDoesNotModifyValue() {
        String value = "persistent-value";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(value);

        wrapper.destroy(Caller.EVICT);
        assertEquals(value, wrapper.value());

        wrapper.destroy(Caller.VACATE);
        assertEquals(value, wrapper.value());

        wrapper.destroy(Caller.REMOVE_REPLACE_CLEAR);
        assertEquals(value, wrapper.value());
    }

    @Test
    public void testHashCodeWithNullValue() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of(null);

        assertEquals(0, wrapper.hashCode());
    }

    @Test
    public void testHashCodeWithNonNullValue() {
        String value = "test";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(value);

        assertEquals(value.hashCode(), wrapper.hashCode());
    }

    @Test
    public void testHashCodeConsistency() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        int firstHash = wrapper.hashCode();
        int secondHash = wrapper.hashCode();

        assertEquals(firstHash, secondHash);
    }

    @Test
    public void testEqualsSameObject() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        assertTrue(wrapper.equals(wrapper));
    }

    @Test
    public void testEqualsWithEqualValues() {
        PoolableWrapper<String> wrapper1 = PoolableWrapper.of("test");
        PoolableWrapper<String> wrapper2 = PoolableWrapper.of("test");

        assertTrue(wrapper1.equals(wrapper2));
        assertTrue(wrapper2.equals(wrapper1));
    }

    @Test
    public void testEqualsWithDifferentValues() {
        PoolableWrapper<String> wrapper1 = PoolableWrapper.of("test1");
        PoolableWrapper<String> wrapper2 = PoolableWrapper.of("test2");

        assertFalse(wrapper1.equals(wrapper2));
        assertFalse(wrapper2.equals(wrapper1));
    }

    @Test
    public void testEqualsWithNull() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        assertFalse(wrapper.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        assertFalse(wrapper.equals("test"));
        assertFalse(wrapper.equals(Integer.valueOf(123)));
    }

    @Test
    public void testEqualsWithBothNullValues() {
        PoolableWrapper<String> wrapper1 = PoolableWrapper.of(null);
        PoolableWrapper<String> wrapper2 = PoolableWrapper.of(null);

        assertTrue(wrapper1.equals(wrapper2));
    }

    @Test
    public void testEqualsWithOneNullValue() {
        PoolableWrapper<String> wrapper1 = PoolableWrapper.of("test");
        PoolableWrapper<String> wrapper2 = PoolableWrapper.of(null);

        assertFalse(wrapper1.equals(wrapper2));
        assertFalse(wrapper2.equals(wrapper1));
    }

    @Test
    public void testToString() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test-value");

        String str = wrapper.toString();

        assertNotNull(str);
        assertTrue(str.contains("srcObject"));
        assertTrue(str.contains("activityPrint"));
    }

    @Test
    public void testToStringWithNullValue() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of(null);

        String str = wrapper.toString();

        assertNotNull(str);
        assertTrue(str.contains("srcObject"));
    }

    @Test
    public void testActivityPrint() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test", 10000, 5000);
        ActivityPrint print = wrapper.activityPrint();

        assertNotNull(print);
        assertEquals(10000, print.getLiveTime());
        assertEquals(5000, print.getMaxIdleTime());
    }

    @Test
    public void testActivityPrintCanBeUpdated() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");
        ActivityPrint print = wrapper.activityPrint();

        int initialCount = print.getAccessCount();
        print.updateAccessCount();

        assertEquals(initialCount + 1, print.getAccessCount());
    }

    @Test
    public void testImplementsPoolableInterface() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        assertTrue(wrapper instanceof Poolable);
    }

    @Test
    public void testImplementsImmutableInterface() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        assertTrue(wrapper instanceof Immutable);
    }

    @Test
    public void testExtendsAbstractPoolable() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test");

        assertTrue(wrapper instanceof AbstractPoolable);
    }

    @Test
    public void testWrapperWithComplexObject() {
        java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
        PoolableWrapper<java.util.List<String>> wrapper = PoolableWrapper.of(list);

        assertNotNull(wrapper);
        assertEquals(list, wrapper.value());
        assertEquals(3, wrapper.value().size());
    }

    @Test
    public void testWrapperImmutability() {
        String value = "immutable-test";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(value);

        // The wrapper itself should be immutable (fields are final)
        assertEquals(value, wrapper.value());

        // Calling destroy should not change the value
        wrapper.destroy(Caller.CLOSE);
        assertEquals(value, wrapper.value());
    }

    @Test
    public void testMultipleWrappersWithSameValue() {
        String value = "shared";
        PoolableWrapper<String> wrapper1 = PoolableWrapper.of(value);
        PoolableWrapper<String> wrapper2 = PoolableWrapper.of(value);

        assertEquals(wrapper1.value(), wrapper2.value());
        assertTrue(wrapper1.equals(wrapper2));
    }

    @Test
    public void testWrappedValueNotExpiredInitially() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test", 10000, 5000);

        assertFalse(wrapper.activityPrint().isExpired());
    }

    @Test
    public void testWrappedValueExpirationWithShortLiveTime() throws InterruptedException {
        PoolableWrapper<String> wrapper = PoolableWrapper.of("test", 1, 5000);

        Thread.sleep(10);

        assertTrue(wrapper.activityPrint().isExpired());
    }

    @Test
    public void testWrapperCreationPreservesType() {
        Integer intValue = 123;
        PoolableWrapper<Integer> wrapper = PoolableWrapper.of(intValue);

        assertTrue(wrapper.value() instanceof Integer);
        assertEquals(intValue, wrapper.value());
    }

    @Test
    public void testHashCodeAndEqualsContract() {
        PoolableWrapper<String> wrapper1 = PoolableWrapper.of("test");
        PoolableWrapper<String> wrapper2 = PoolableWrapper.of("test");

        if (wrapper1.equals(wrapper2)) {
            assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
        }
    }
}
