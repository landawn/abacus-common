package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ActivityPrint100Test extends TestBase {

    private ActivityPrint activityPrint;
    private static final long LIVE_TIME = 10000;
    private static final long MAX_IDLE_TIME = 5000;

    @BeforeEach
    public void setUp() {
        activityPrint = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME);
    }

    @Test
    public void testConstructor() {
        assertNotNull(activityPrint);
        assertEquals(LIVE_TIME, activityPrint.getLiveTime());
        assertEquals(MAX_IDLE_TIME, activityPrint.getMaxIdleTime());
        assertEquals(0, activityPrint.getAccessCount());
        assertTrue(activityPrint.getCreatedTime() > 0);
        assertEquals(activityPrint.getCreatedTime(), activityPrint.getLastAccessTime());
    }

    @Test
    public void testConstructorWithInvalidLiveTime() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(0, MAX_IDLE_TIME));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(-1, MAX_IDLE_TIME));
    }

    @Test
    public void testConstructorWithInvalidMaxIdleTime() {
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(LIVE_TIME, 0));
        assertThrows(IllegalArgumentException.class, () -> new ActivityPrint(LIVE_TIME, -1));
    }

    @Test
    public void testValueOf() {
        ActivityPrint ap = ActivityPrint.of(LIVE_TIME, MAX_IDLE_TIME);
        assertNotNull(ap);
        assertEquals(LIVE_TIME, ap.getLiveTime());
        assertEquals(MAX_IDLE_TIME, ap.getMaxIdleTime());
    }

    @Test
    public void testGetLiveTime() {
        assertEquals(LIVE_TIME, activityPrint.getLiveTime());
    }

    @Test
    public void testSetLiveTime() {
        long newLiveTime = 20000;
        ActivityPrint result = activityPrint.setLiveTime(newLiveTime);
        assertSame(activityPrint, result);
        assertEquals(newLiveTime, activityPrint.getLiveTime());
    }

    @Test
    public void testSetLiveTimeWithNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> activityPrint.setLiveTime(-1));
    }

    @Test
    public void testGetMaxIdleTime() {
        assertEquals(MAX_IDLE_TIME, activityPrint.getMaxIdleTime());
    }

    @Test
    public void testSetMaxIdleTime() {
        long newMaxIdleTime = 8000;
        ActivityPrint result = activityPrint.setMaxIdleTime(newMaxIdleTime);
        assertSame(activityPrint, result);
        assertEquals(newMaxIdleTime, activityPrint.getMaxIdleTime());
    }

    @Test
    public void testSetMaxIdleTimeWithNegativeValue() {
        assertThrows(IllegalArgumentException.class, () -> activityPrint.setMaxIdleTime(-1));
    }

    @Test
    public void testGetCreatedTime() {
        long createdTime = activityPrint.getCreatedTime();
        assertTrue(createdTime > 0);
        assertTrue(createdTime <= System.currentTimeMillis());
    }

    @Test
    public void testGetLastAccessTime() {
        long initialLastAccessTime = activityPrint.getLastAccessTime();
        assertEquals(activityPrint.getCreatedTime(), initialLastAccessTime);
    }

    @Test
    public void testUpdateLastAccessTime() throws InterruptedException {
        long initialLastAccessTime = activityPrint.getLastAccessTime();
        Thread.sleep(10);
        activityPrint.updateLastAccessTime();
        long updatedLastAccessTime = activityPrint.getLastAccessTime();
        assertTrue(updatedLastAccessTime > initialLastAccessTime);
    }

    @Test
    public void testGetAccessCount() {
        assertEquals(0, activityPrint.getAccessCount());
    }

    @Test
    public void testUpdateAccessCount() {
        assertEquals(0, activityPrint.getAccessCount());
        activityPrint.updateAccessCount();
        assertEquals(1, activityPrint.getAccessCount());
        activityPrint.updateAccessCount();
        assertEquals(2, activityPrint.getAccessCount());
    }

    @Test
    public void testGetExpirationTime() {
        long expectedExpiration = activityPrint.getCreatedTime() + LIVE_TIME;
        assertEquals(expectedExpiration, activityPrint.getExpirationTime());
    }

    @Test
    public void testGetExpirationTimeWithOverflow() {
        activityPrint.setCreatedTime(Long.MAX_VALUE - 1000);
        activityPrint.setLiveTime(2000);
        assertEquals(Long.MAX_VALUE, activityPrint.getExpirationTime());
    }

    @Test
    public void testIsExpiredByLiveTime() throws InterruptedException {
        ActivityPrint shortLivedPrint = new ActivityPrint(50, 1000);
        assertFalse(shortLivedPrint.isExpired());
        Thread.sleep(60);
        assertTrue(shortLivedPrint.isExpired());
    }

    @Test
    public void testIsExpiredByIdleTime() throws InterruptedException {
        ActivityPrint shortIdlePrint = new ActivityPrint(1000, 50);
        assertFalse(shortIdlePrint.isExpired());
        Thread.sleep(60);
        assertTrue(shortIdlePrint.isExpired());
    }

    @Test
    public void testIsNotExpired() {
        assertFalse(activityPrint.isExpired());
    }

    @Test
    public void testClone() {
        activityPrint.updateAccessCount();
        activityPrint.updateAccessCount();

        Object cloned = activityPrint.clone();
        assertNotNull(cloned);
        assertInstanceOf(ActivityPrint.class, cloned);

        ActivityPrint clonedPrint = (ActivityPrint) cloned;
        assertNotSame(activityPrint, clonedPrint);
        assertEquals(activityPrint.getLiveTime(), clonedPrint.getLiveTime());
        assertEquals(activityPrint.getMaxIdleTime(), clonedPrint.getMaxIdleTime());
        assertEquals(activityPrint.getCreatedTime(), clonedPrint.getCreatedTime());
        assertEquals(activityPrint.getLastAccessTime(), clonedPrint.getLastAccessTime());
        assertEquals(activityPrint.getAccessCount(), clonedPrint.getAccessCount());
    }

    @Test
    public void testHashCode() {
        ActivityPrint ap1 = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME);
        ActivityPrint ap2 = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME);

        int hash1 = activityPrint.hashCode();
        int hash2 = activityPrint.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquals() {
        ActivityPrint ap1 = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME);
        ActivityPrint ap2 = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME);

        assertEquals(ap1, ap1);

        assertNotEquals(ap1, null);

        assertNotEquals(ap1, "not an ActivityPrint");

        assertEquals(ap1, ap2);

        ActivityPrint cloned = (ActivityPrint) ap1.clone();
        assertEquals(ap1, cloned);
    }

    @Test
    public void testEqualsWithDifferentFields() {
        ActivityPrint ap1 = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME);
        ActivityPrint ap2 = new ActivityPrint(LIVE_TIME + 1000, MAX_IDLE_TIME);
        assertNotEquals(ap1, ap2);

        ap2 = new ActivityPrint(LIVE_TIME, MAX_IDLE_TIME + 1000);
        assertNotEquals(ap1, ap2);
    }

    @Test
    public void testToString() {
        String str = activityPrint.toString();
        assertNotNull(str);
        assertTrue(str.contains("createdTime="));
        assertTrue(str.contains("liveTime=" + LIVE_TIME));
        assertTrue(str.contains("maxIdleTime=" + MAX_IDLE_TIME));
        assertTrue(str.contains("lastAccessedTime="));
        assertTrue(str.contains("accessCount=0"));
    }

    @Test
    public void testSetCreatedTime() {
        long newCreatedTime = System.currentTimeMillis() - 1000;
        ActivityPrint result = activityPrint.setCreatedTime(newCreatedTime);
        assertSame(activityPrint, result);
        assertEquals(newCreatedTime, activityPrint.getCreatedTime());
    }
}
