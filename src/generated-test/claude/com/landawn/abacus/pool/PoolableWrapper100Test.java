package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class PoolableWrapper100Test extends TestBase {

    @Test
    public void testConstructorWithObjectOnly() {
        String testObject = "test";
        PoolableWrapper<String> wrapper = new PoolableWrapper<>(testObject);
        
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
        
        PoolableWrapper<Integer> wrapper = new PoolableWrapper<>(testObject, liveTime, maxIdleTime);
        
        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertNotNull(wrapper.activityPrint());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testConstructorWithNullObject() {
        PoolableWrapper<Object> wrapper = new PoolableWrapper<>(null);
        assertNotNull(wrapper);
        assertNull(wrapper.value());
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
    public void testOfWithObjectOnly() {
        String testObject = "test";
        PoolableWrapper<String> wrapper = PoolableWrapper.of(testObject);
        
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
        
        PoolableWrapper<Double> wrapper = PoolableWrapper.of(testObject, liveTime, maxIdleTime);
        
        assertNotNull(wrapper);
        assertEquals(testObject, wrapper.value());
        assertEquals(liveTime, wrapper.activityPrint().getLiveTime());
        assertEquals(maxIdleTime, wrapper.activityPrint().getMaxIdleTime());
    }

    @Test
    public void testOfWithNull() {
        PoolableWrapper<String> wrapper = PoolableWrapper.of(null);
        assertNotNull(wrapper);
        assertNull(wrapper.value());
    }

    @Test
    public void testValue() {
        String testObject = "test value";
        PoolableWrapper<String> wrapper = new PoolableWrapper<>(testObject);
        assertEquals(testObject, wrapper.value());
        
        // Test that the same instance is returned
        assertSame(testObject, wrapper.value());
    }

    @Test
    public void testActivityPrint() {
        PoolableWrapper<String> wrapper = new PoolableWrapper<>("test", 10000, 5000);
        ActivityPrint activityPrint = wrapper.activityPrint();
        
        assertNotNull(activityPrint);
        assertEquals(10000, activityPrint.getLiveTime());
        assertEquals(5000, activityPrint.getMaxIdleTime());
        
        // Test that same instance is returned
        assertSame(activityPrint, wrapper.activityPrint());
    }

    @Test
    public void testDestroy() {
        String testObject = "test";
        PoolableWrapper<String> wrapper = new PoolableWrapper<>(testObject);
        
        // Destroy should be a no-op and not affect the wrapped object
        wrapper.destroy(Poolable.Caller.CLOSE);
        assertEquals(testObject, wrapper.value());
        
        wrapper.destroy(Poolable.Caller.EVICT);
        assertEquals(testObject, wrapper.value());
        
        // Test all callers
        for (Poolable.Caller caller : Poolable.Caller.values()) {
            wrapper.destroy(caller);
            assertEquals(testObject, wrapper.value());
        }
    }

    @Test
    public void testHashCode() {
        String testObject = "test";
        PoolableWrapper<String> wrapper1 = new PoolableWrapper<>(testObject);
        PoolableWrapper<String> wrapper2 = new PoolableWrapper<>(testObject);
        
        assertEquals(wrapper1.hashCode(), wrapper2.hashCode());
        
        PoolableWrapper<String> wrapperNull = new PoolableWrapper<>(null);
        assertEquals(0, wrapperNull.hashCode());
        
        PoolableWrapper<String> wrapperDifferent = new PoolableWrapper<>("different");
        assertNotEquals(wrapper1.hashCode(), wrapperDifferent.hashCode());
    }

    @Test
    public void testEquals() {
        String testObject = "test";
        PoolableWrapper<String> wrapper1 = new PoolableWrapper<>(testObject);
        PoolableWrapper<String> wrapper2 = new PoolableWrapper<>(testObject);
        PoolableWrapper<String> wrapper3 = new PoolableWrapper<>("different");
        
        // Test reflexivity
        assertEquals(wrapper1, wrapper1);
        
        // Test equality with same wrapped object
        assertEquals(wrapper1, wrapper2);
        assertEquals(wrapper2, wrapper1);
        
        // Test inequality
        assertNotEquals(wrapper1, wrapper3);
        assertNotEquals(wrapper3, wrapper1);
        
        // Test null
        assertNotEquals(wrapper1, null);
        
        // Test different type
        assertNotEquals(wrapper1, "not a wrapper");
        
        // Test with null wrapped objects
        PoolableWrapper<String> wrapperNull1 = new PoolableWrapper<>(null);
        PoolableWrapper<String> wrapperNull2 = new PoolableWrapper<>(null);
        assertEquals(wrapperNull1, wrapperNull2);
    }

    @Test
    public void testEqualsWithDifferentLiveTimes() {
        String testObject = "test";
        PoolableWrapper<String> wrapper1 = new PoolableWrapper<>(testObject, 10000, 5000);
        PoolableWrapper<String> wrapper2 = new PoolableWrapper<>(testObject, 20000, 5000);
        
        // Wrappers are equal if they wrap the same object, regardless of activity print
        assertEquals(wrapper1, wrapper2);
    }

    @Test
    public void testToString() {
        String testObject = "test";
        PoolableWrapper<String> wrapper = new PoolableWrapper<>(testObject, 10000, 5000);
        
        String str = wrapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("srcObject=" + testObject));
        assertTrue(str.contains("activityPrint="));
    }

    @Test
    public void testToStringWithNull() {
        PoolableWrapper<Object> wrapper = new PoolableWrapper<>(null);
        String str = wrapper.toString();
        assertNotNull(str);
        assertTrue(str.contains("srcObject=null"));
    }

    @Test
    public void testImmutability() {
        String testObject = "test";
        PoolableWrapper<String> wrapper = new PoolableWrapper<>(testObject);
        
        // The wrapper should maintain the same value even after destroy
        String valueBefore = wrapper.value();
        wrapper.destroy(Poolable.Caller.CLOSE);
        String valueAfter = wrapper.value();
        
        assertSame(valueBefore, valueAfter);
    }

    @Test
    public void testWithComplexObjects() {
        // Test with a complex object type
        ComplexObject obj = new ComplexObject("name", 123);
        PoolableWrapper<ComplexObject> wrapper = PoolableWrapper.of(obj);
        
        assertNotNull(wrapper);
        assertSame(obj, wrapper.value());
        assertEquals("ComplexObject[name=name, value=123]", wrapper.value().toString());
    }

    private static class ComplexObject {
        private final String name;
        private final int value;

        ComplexObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return "ComplexObject[name=" + name + ", value=" + value + "]";
        }
    }
}
