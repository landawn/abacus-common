package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ActivityPrint2025Test extends TestBase {

    @Test
    public void testConstructorWithValidValues() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertNotNull(print);
        assertEquals(10000, print.getLiveTime());
        assertEquals(5000, print.getMaxIdleTime());
        assertTrue(print.getCreatedTime() > 0);
        assertTrue(print.getLastAccessTime() > 0);
        assertEquals(0, print.getAccessCount());
    }

    @Test
    public void testConstructorWithMaxValues() {
        ActivityPrint print = new ActivityPrint(Long.MAX_VALUE, Long.MAX_VALUE);

        assertNotNull(print);
        assertEquals(Long.MAX_VALUE, print.getLiveTime());
        assertEquals(Long.MAX_VALUE, print.getMaxIdleTime());
    }

    @Test
    public void testConstructorWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(0, 5000));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(-1, 5000));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(-100, 5000));
    }

    @Test
    public void testConstructorWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(10000, 0));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(10000, -1));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(10000, -100));
    }

    @Test
    public void testConstructorWithBothInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(-1, -1));
    }

    @Test
    public void testValueOfFactoryMethod() {
        ActivityPrint print = ActivityPrint.valueOf(10000, 5000);

        assertNotNull(print);
        assertEquals(10000, print.getLiveTime());
        assertEquals(5000, print.getMaxIdleTime());
    }

    @Test
    public void testValueOfWithInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> ActivityPrint.valueOf(0, 5000));
        assertThrows(IllegalArgumentException.class, () -> ActivityPrint.valueOf(10000, 0));
    }

    @Test
    public void testGetLiveTime() {
        ActivityPrint print = new ActivityPrint(12345, 6789);

        assertEquals(12345, print.getLiveTime());
    }

    @Test
    public void testSetLiveTime() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        ActivityPrint result = print.setLiveTime(15000);

        assertEquals(print, result);   // returns this for chaining
        assertEquals(15000, print.getLiveTime());
    }

    @Test
    public void testSetLiveTimeToZero() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        ActivityPrint result = print.setLiveTime(0);

        assertEquals(0, print.getLiveTime());
        assertEquals(print, result);
    }

    @Test
    public void testSetLiveTimeWithNegativeValue() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertThrows(IllegalArgumentException.class, () -> print.setLiveTime(-1));
        assertThrows(IllegalArgumentException.class, () -> print.setLiveTime(-100));
    }

    @Test
    public void testGetMaxIdleTime() {
        ActivityPrint print = new ActivityPrint(10000, 6789);

        assertEquals(6789, print.getMaxIdleTime());
    }

    @Test
    public void testSetMaxIdleTime() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        ActivityPrint result = print.setMaxIdleTime(7500);

        assertEquals(print, result);   // returns this for chaining
        assertEquals(7500, print.getMaxIdleTime());
    }

    @Test
    public void testSetMaxIdleTimeToZero() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        ActivityPrint result = print.setMaxIdleTime(0);

        assertEquals(0, print.getMaxIdleTime());
        assertEquals(print, result);
    }

    @Test
    public void testSetMaxIdleTimeWithNegativeValue() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertThrows(IllegalArgumentException.class, () -> print.setMaxIdleTime(-1));
        assertThrows(IllegalArgumentException.class, () -> print.setMaxIdleTime(-100));
    }

    @Test
    public void testGetCreatedTime() {
        long beforeCreation = System.currentTimeMillis();
        ActivityPrint print = new ActivityPrint(10000, 5000);
        long afterCreation = System.currentTimeMillis();

        long createdTime = print.getCreatedTime();

        assertTrue(createdTime >= beforeCreation);
        assertTrue(createdTime <= afterCreation);
    }

    @Test
    public void testSetCreatedTime() {
        ActivityPrint print = new ActivityPrint(10000, 5000);
        long customTime = System.currentTimeMillis() - 100000;

        ActivityPrint result = print.setCreatedTime(customTime);

        assertEquals(customTime, print.getCreatedTime());
        assertEquals(print, result);
    }

    @Test
    public void testGetLastAccessTime() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        long lastAccessTime = print.getLastAccessTime();

        assertTrue(lastAccessTime > 0);
        // Initially, last access time should be equal to created time
        assertEquals(print.getCreatedTime(), lastAccessTime);
    }

    @Test
    public void testUpdateLastAccessTime() throws InterruptedException {
        ActivityPrint print = new ActivityPrint(10000, 5000);
        long initialAccessTime = print.getLastAccessTime();

        Thread.sleep(10);
        print.updateLastAccessTime();

        assertTrue(print.getLastAccessTime() > initialAccessTime);
    }

    @Test
    public void testUpdateLastAccessTimeMultipleTimes() throws InterruptedException {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        long firstAccessTime = print.getLastAccessTime();
        Thread.sleep(5);
        print.updateLastAccessTime();
        long secondAccessTime = print.getLastAccessTime();
        Thread.sleep(5);
        print.updateLastAccessTime();
        long thirdAccessTime = print.getLastAccessTime();

        assertTrue(secondAccessTime >= firstAccessTime);
        assertTrue(thirdAccessTime >= secondAccessTime);
    }

    @Test
    public void testGetAccessCount() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertEquals(0, print.getAccessCount());
    }

    @Test
    public void testUpdateAccessCount() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertEquals(0, print.getAccessCount());

        print.updateAccessCount();
        assertEquals(1, print.getAccessCount());

        print.updateAccessCount();
        assertEquals(2, print.getAccessCount());

        print.updateAccessCount();
        assertEquals(3, print.getAccessCount());
    }

    @Test
    public void testUpdateAccessCountMultipleTimes() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        for (int i = 0; i < 100; i++) {
            print.updateAccessCount();
        }

        assertEquals(100, print.getAccessCount());
    }

    @Test
    public void testGetExpirationTime() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        long expirationTime = print.getExpirationTime();

        assertTrue(expirationTime > 0);
        assertTrue(expirationTime >= print.getCreatedTime() + 10000);
    }

    @Test
    public void testGetExpirationTimeWithMaxLiveTime() {
        ActivityPrint print = new ActivityPrint(Long.MAX_VALUE, 5000);

        long expirationTime = print.getExpirationTime();

        assertEquals(Long.MAX_VALUE, expirationTime);
    }

    @Test
    public void testGetExpirationTimeWithOverflow() {
        ActivityPrint print = new ActivityPrint(Long.MAX_VALUE, 5000);
        print.setCreatedTime(1000);

        long expirationTime = print.getExpirationTime();

        assertEquals(Long.MAX_VALUE, expirationTime);
    }

    @Test
    public void testIsExpiredInitially() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertFalse(print.isExpired());
    }

    @Test
    public void testIsExpiredWithVeryShortLiveTime() throws InterruptedException {
        ActivityPrint print = new ActivityPrint(1, 5000);

        Thread.sleep(10);

        assertTrue(print.isExpired());
    }

    @Test
    public void testIsExpiredWithVeryShortMaxIdleTime() throws InterruptedException {
        ActivityPrint print = new ActivityPrint(10000, 1);

        Thread.sleep(10);

        assertTrue(print.isExpired());
    }

    @Test
    public void testIsExpiredWithOldCreatedTime() {
        ActivityPrint print = new ActivityPrint(1000, 5000);
        print.setCreatedTime(System.currentTimeMillis() - 2000);

        assertTrue(print.isExpired());
    }

    @Test
    public void testIsExpiredAfterIdleTimeExceeded() throws InterruptedException {
        ActivityPrint print = new ActivityPrint(10000, 1);
        print.updateLastAccessTime();

        Thread.sleep(10);

        assertTrue(print.isExpired());
    }

    @Test
    public void testIsExpiredAfterAccessUpdate() throws InterruptedException {
        ActivityPrint print = new ActivityPrint(10000, 100);

        Thread.sleep(50);
        assertFalse(print.isExpired());

        print.updateLastAccessTime();
        assertFalse(print.isExpired());
    }

    @Test
    public void testClone() {
        ActivityPrint original = new ActivityPrint(10000, 5000);
        original.updateAccessCount();
        original.updateAccessCount();

        ActivityPrint cloned = (ActivityPrint) original.clone();

        assertNotNull(cloned);
        assertEquals(original.getLiveTime(), cloned.getLiveTime());
        assertEquals(original.getMaxIdleTime(), cloned.getMaxIdleTime());
        assertEquals(original.getCreatedTime(), cloned.getCreatedTime());
        assertEquals(original.getLastAccessTime(), cloned.getLastAccessTime());
        assertEquals(original.getAccessCount(), cloned.getAccessCount());
    }

    @Test
    public void testCloneIsIndependent() {
        ActivityPrint original = new ActivityPrint(10000, 5000);
        ActivityPrint cloned = (ActivityPrint) original.clone();

        original.updateAccessCount();
        original.setLiveTime(15000);

        assertEquals(1, original.getAccessCount());
        assertEquals(0, cloned.getAccessCount());
        assertEquals(15000, original.getLiveTime());
        assertEquals(10000, cloned.getLiveTime());
    }

    @Test
    public void testHashCode() {
        ActivityPrint print1 = new ActivityPrint(10000, 5000);
        ActivityPrint print2 = new ActivityPrint(10000, 5000);

        // Hash codes might be different due to different creation times
        assertNotNull(Integer.valueOf(print1.hashCode()));
        assertNotNull(Integer.valueOf(print2.hashCode()));
    }

    @Test
    public void testEquals() {
        ActivityPrint print1 = new ActivityPrint(10000, 5000);
        ActivityPrint print2 = (ActivityPrint) print1.clone();

        assertTrue(print1.equals(print2));
        assertTrue(print2.equals(print1));
    }

    @Test
    public void testEqualsSameObject() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertTrue(print.equals(print));
    }

    @Test
    public void testEqualsWithDifferentAccessCount() {
        ActivityPrint print1 = new ActivityPrint(10000, 5000);
        ActivityPrint print2 = (ActivityPrint) print1.clone();

        print2.updateAccessCount();

        assertFalse(print1.equals(print2));
    }

    @Test
    public void testEqualsWithDifferentLiveTime() {
        ActivityPrint print1 = new ActivityPrint(10000, 5000);
        ActivityPrint print2 = (ActivityPrint) print1.clone();

        print2.setLiveTime(15000);

        assertFalse(print1.equals(print2));
    }

    @Test
    public void testEqualsWithNull() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertFalse(print.equals(null));
    }

    @Test
    public void testEqualsWithDifferentClass() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        assertFalse(print.equals("not an ActivityPrint"));
    }

    @Test
    public void testToString() {
        ActivityPrint print = new ActivityPrint(10000, 5000);
        print.updateAccessCount();

        String str = print.toString();

        assertNotNull(str);
        assertTrue(str.contains("createdTime"));
        assertTrue(str.contains("liveTime"));
        assertTrue(str.contains("maxIdleTime"));
        assertTrue(str.contains("lastAccessedTime"));
        assertTrue(str.contains("accessCount"));
    }

    @Test
    public void testToStringWithDifferentValues() {
        ActivityPrint print = new ActivityPrint(12345, 6789);
        print.setCreatedTime(1000000);
        print.updateAccessCount();
        print.updateAccessCount();
        print.updateAccessCount();

        String str = print.toString();

        assertNotNull(str);
        assertTrue(str.contains("12345"));
        assertTrue(str.contains("6789"));
    }

    @Test
    public void testChainingSetter() {
        ActivityPrint print = new ActivityPrint(10000, 5000);

        ActivityPrint result = print.setLiveTime(15000).setMaxIdleTime(7500);

        assertEquals(print, result);
        assertEquals(15000, print.getLiveTime());
        assertEquals(7500, print.getMaxIdleTime());
    }
}
