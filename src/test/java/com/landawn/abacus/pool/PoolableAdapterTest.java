package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.pool.Poolable.Caller;
import com.landawn.abacus.util.Immutable;

public class PoolableAdapterTest extends TestBase {

    @Test
    public void testActivityPrintCanBeUpdated() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");
        ActivityPrint print = wrapper.activityPrint();

        int initialCount = print.getAccessCount();
        print.updateAccessCount();

        assertEquals(initialCount + 1, print.getAccessCount());
    }

    @Test
    public void testImplementsPoolableInterface() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        assertTrue(wrapper instanceof Poolable);
    }

    @Test
    public void testImplementsImmutableInterface() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        assertTrue(wrapper instanceof Immutable);
    }

    @Test
    public void testExtendsAbstractPoolable() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        assertTrue(wrapper instanceof AbstractPoolable);
    }

    @Test
    public void testWrappedValueNotExpiredInitially() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test", 10000, 5000);

        assertFalse(wrapper.activityPrint().isExpired());
    }

    @Test
    public void testWrapperCreationPreservesType() {
        Integer intValue = 123;
        PoolableAdapter<Integer> wrapper = PoolableAdapter.of(intValue);

        assertTrue(wrapper.value() instanceof Integer);
        assertEquals(intValue, wrapper.value());
    }

    @Test
    public void testOfFactoryMethodWithDefaultValues() {
        String value = "factory-test";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(value);

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

        PoolableAdapter<Double> wrapper = PoolableAdapter.of(value, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(value, wrapper.value());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testOfWithNullValue() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of(null);

        assertNotNull(wrapper);
        assertNull(wrapper.value());
    }

    @Test
    public void testActivityPrint() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test", 10000, 5000);
        ActivityPrint print = wrapper.activityPrint();

        assertNotNull(print);
        assertEquals(10000, print.getLiveTime());
        assertEquals(5000, print.getMaxIdleTime());
    }

    @Test
    public void testWrapperWithComplexObject() {
        java.util.List<String> list = java.util.Arrays.asList("a", "b", "c");
        PoolableAdapter<java.util.List<String>> wrapper = PoolableAdapter.of(list);

        assertNotNull(wrapper);
        assertEquals(list, wrapper.value());
        assertEquals(3, wrapper.value().size());
    }

    @Test
    public void testWrapperImmutability() {
        String value = "immutable-test";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(value);

        // The wrapper itself should be immutable (fields are final)
        assertEquals(value, wrapper.value());

        // Calling destroy should not change the value
        wrapper.destroy(Caller.CLOSE);
        assertEquals(value, wrapper.value());
    }

    @Test
    public void testMultipleWrappersWithSameValue() {
        String value = "shared";
        PoolableAdapter<String> wrapper1 = PoolableAdapter.of(value);
        PoolableAdapter<String> wrapper2 = PoolableAdapter.of(value);

        assertEquals(wrapper1.value(), wrapper2.value());
        assertTrue(wrapper1.equals(wrapper2));
    }

    @Test
    public void testOfWithObjectOnly() {
        String testObject = "test";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(testObject);

        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getLiveTime());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testOfWithAllParameters() {
        Double testObject = 3.14;
        long liveTime = 20000;
        long maxIdleTime = 10000;

        PoolableAdapter<Double> wrapper = PoolableAdapter.of(testObject, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testWrappedValueExpirationWithShortLiveTime() throws InterruptedException {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test", 1, 5000);

        Thread.sleep(10);

        assertTrue(wrapper.activityPrint().isExpired());
    }

    @Test
    public void testValue() {
        String expectedValue = "wrapped-value";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(expectedValue);

        assertEquals(expectedValue, wrapper.value());
    }

    @Test
    public void testValueWithDifferentTypes() {
        PoolableAdapter<Integer> intWrapper = PoolableAdapter.of(123);
        assertEquals(Integer.valueOf(123), intWrapper.value());

        PoolableAdapter<String> stringWrapper = PoolableAdapter.of("hello");
        assertEquals("hello", stringWrapper.value());

        PoolableAdapter<Boolean> boolWrapper = PoolableAdapter.of(true);
        assertEquals(Boolean.TRUE, boolWrapper.value());
    }

    @Test
    public void testConstructorWithDefaultValues() {
        String value = "test-value";
        PoolableAdapter<String> wrapper = new PoolableAdapter<>(value);

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

        PoolableAdapter<Integer> wrapper = new PoolableAdapter<>(value, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(value, wrapper.value());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithNullValue() {
        PoolableAdapter<Object> wrapper = new PoolableAdapter<>(null);

        assertNotNull(wrapper);
        assertNull(wrapper.value());
        assertNotNull(wrapper.activityPrint());
    }

    @Test
    public void testValueWithNullWrappedObject() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of(null);

        assertNull(wrapper.value());
    }

    @Test
    public void testConstructorWithObjectOnly() {
        String testObject = "test";
        PoolableAdapter<String> wrapper = new PoolableAdapter<>(testObject);

        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertNotNull(wrapper.activityPrint());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getLiveTime());
        assertEquals(Long.MAX_VALUE, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithAllParameters() {
        Integer testObject = 42;
        long liveTime = 10000;
        long maxIdleTime = 5000;

        PoolableAdapter<Integer> wrapper = new PoolableAdapter<>(testObject, liveTime, maxIdleTime);

        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertNotNull(wrapper.activityPrint());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithNullObject() {
        PoolableAdapter<Object> wrapper = new PoolableAdapter<>(null);
        assertNotNull(wrapper);
        assertNull(wrapper.value());
    }

    @Test
    public void testImmutability() {
        String testObject = "test";
        PoolableAdapter<String> wrapper = new PoolableAdapter<>(testObject);

        String valueBefore = wrapper.value();
        wrapper.destroy(Poolable.Caller.CLOSE);
        String valueAfter = wrapper.value();

        assertSame(valueBefore, valueAfter);
    }

    @Test
    public void testDestroy() {
        String value = "test";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(value);

        // destroy should be a no-op
        wrapper.destroy(Caller.CLOSE);

        // value should still be accessible
        assertEquals(value, wrapper.value());
    }

    @Test
    public void testDestroyDoesNotModifyValue() {
        String value = "persistent-value";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(value);

        wrapper.destroy(Caller.EVICT);
        assertEquals(value, wrapper.value());

        wrapper.destroy(Caller.VACATE);
        assertEquals(value, wrapper.value());

        wrapper.destroy(Caller.REMOVE_REPLACE_CLEAR);
        assertEquals(value, wrapper.value());
    }

    @Test
    public void testDestroyWithDifferentCallers() {
        for (Caller caller : Caller.values()) {
            PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

            wrapper.destroy(caller);

            // Value should still be accessible after destroy
            assertNotNull(wrapper.value());
            assertEquals("test", wrapper.value());
        }
    }

    @Test
    public void testHashCodeConsistency() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        int firstHash = wrapper.hashCode();
        int secondHash = wrapper.hashCode();

        assertEquals(firstHash, secondHash);
    }

    @Test
    public void testHashCodeAndEqualsContract() {
        PoolableAdapter<String> wrapper1 = PoolableAdapter.of("test");
        PoolableAdapter<String> wrapper2 = PoolableAdapter.of("test");

        if (wrapper1.equals(wrapper2)) {
            assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
        }
    }

    @Test
    public void testHashCodeWithNullValue() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of(null);

        assertEquals(0, wrapper.hashCode());
    }

    @Test
    public void testHashCodeWithNonNullValue() {
        String value = "test";
        PoolableAdapter<String> wrapper = PoolableAdapter.of(value);

        assertEquals(value.hashCode(), wrapper.hashCode());
    }

    @Test
    public void testHashCode() {
        String testObject = "test";
        PoolableAdapter<String> wrapper1 = new PoolableAdapter<>(testObject);
        PoolableAdapter<String> wrapper2 = new PoolableAdapter<>(testObject);

        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());

        PoolableAdapter<String> wrapperNull = new PoolableAdapter<>(null);
        assertEquals(0, wrapperNull.hashCode());

        PoolableAdapter<String> wrapperDifferent = new PoolableAdapter<>("different");
        assertNotEquals(wrapper1.hashCode(), wrapperDifferent.hashCode());
    }

    @Test
    public void testEqualsWithEqualValues() {
        PoolableAdapter<String> wrapper1 = PoolableAdapter.of("test");
        PoolableAdapter<String> wrapper2 = PoolableAdapter.of("test");

        assertTrue(wrapper1.equals(wrapper2));
        assertTrue(wrapper2.equals(wrapper1));
    }

    @Test
    public void testEqualsWithDifferentValues() {
        PoolableAdapter<String> wrapper1 = PoolableAdapter.of("test1");
        PoolableAdapter<String> wrapper2 = PoolableAdapter.of("test2");

        assertFalse(wrapper1.equals(wrapper2));
        assertFalse(wrapper2.equals(wrapper1));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        assertFalse(wrapper.equals("test"));
        assertFalse(wrapper.equals(Integer.valueOf(123)));
    }

    @Test
    public void testEqualsWithDifferentLiveTimes() {
        String testObject = "test";
        PoolableAdapter<String> wrapper1 = new PoolableAdapter<>(testObject, 10000, 5000);
        PoolableAdapter<String> wrapper2 = new PoolableAdapter<>(testObject, 20000, 5000);

        assertEquals(wrapper1, wrapper2);
    }

    @Test
    public void testEqualsSameObject() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        assertTrue(wrapper.equals(wrapper));
    }

    @Test
    public void testEqualsWithNull() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test");

        assertFalse(wrapper.equals(null));
    }

    @Test
    public void testEqualsWithBothNullValues() {
        PoolableAdapter<String> wrapper1 = PoolableAdapter.of(null);
        PoolableAdapter<String> wrapper2 = PoolableAdapter.of(null);

        assertTrue(wrapper1.equals(wrapper2));
    }

    @Test
    public void testEqualsWithOneNullValue() {
        PoolableAdapter<String> wrapper1 = PoolableAdapter.of("test");
        PoolableAdapter<String> wrapper2 = PoolableAdapter.of(null);

        assertFalse(wrapper1.equals(wrapper2));
        assertFalse(wrapper2.equals(wrapper1));
    }

    @Test
    public void testEquals() {
        String testObject = "test";
        PoolableAdapter<String> wrapper1 = new PoolableAdapter<>(testObject);
        PoolableAdapter<String> wrapper2 = new PoolableAdapter<>(testObject);
        PoolableAdapter<String> wrapper3 = new PoolableAdapter<>("different");

        assertEquals(wrapper1, wrapper1);

        assertEquals(wrapper1, wrapper2);
        assertEquals(wrapper2, wrapper1);

        assertNotEquals(wrapper1, wrapper3);
        assertNotEquals(wrapper3, wrapper1);

        assertNotEquals(wrapper1, null);

        assertNotEquals(wrapper1, "not a wrapper");

        PoolableAdapter<String> wrapperNull1 = new PoolableAdapter<>(null);
        PoolableAdapter<String> wrapperNull2 = new PoolableAdapter<>(null);
        assertEquals(wrapperNull1, wrapperNull2);
    }

    @Test
    public void testToString() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of("test-value");

        String str = wrapper.toString();

        assertNotNull(str);
        assertTrue(str.contains("srcObject"));
        assertTrue(str.contains("activityPrint"));
    }

    @Test
    public void testToStringWithNullValue() {
        PoolableAdapter<String> wrapper = PoolableAdapter.of(null);

        String str = wrapper.toString();

        assertNotNull(str);
        assertTrue(str.contains("srcObject"));
    }

    @Test
    public void testToStringWithNull() {
        PoolableAdapter<Object> wrapper = new PoolableAdapter<>(null);
        String str = wrapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("srcObject=null"));
    }

    @Test
    public void testConstructorWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> new PoolableAdapter<>("test", 0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new PoolableAdapter<>("test", -1, 5000));
    }

    @Test
    public void testConstructorWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> new PoolableAdapter<>("test", 5000, 0));
        assertThrows(IllegalArgumentException.class, () -> new PoolableAdapter<>("test", 5000, -1));
    }

}
