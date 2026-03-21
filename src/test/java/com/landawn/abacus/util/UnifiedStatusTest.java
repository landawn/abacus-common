package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class UnifiedStatusTest extends TestBase {

    @Test
    public void testCode() {
        assertEquals(0, UnifiedStatus.BLANK.code());
        assertEquals(1, UnifiedStatus.ACTIVE.code());
        assertEquals(2, UnifiedStatus.SUSPENDED.code());
        assertEquals(3, UnifiedStatus.EXPIRED.code());
        assertEquals(4, UnifiedStatus.CANCELED.code());
        assertEquals(5, UnifiedStatus.REFUNDED.code());
        assertEquals(6, UnifiedStatus.COMPLETED.code());
        assertEquals(7, UnifiedStatus.CLOSED.code());
        assertEquals(8, UnifiedStatus.REMOVED.code());
        assertEquals(9, UnifiedStatus.DELETED.code());
        assertEquals(11, UnifiedStatus.CONCLUDED.code());
        assertEquals(12, UnifiedStatus.REVOKED.code());
        assertEquals(13, UnifiedStatus.TERMINATED.code());
        assertEquals(14, UnifiedStatus.RETIRED.code());
        assertEquals(15, UnifiedStatus.DESTROYED.code());
        assertEquals(21, UnifiedStatus.STARTED.code());
        assertEquals(22, UnifiedStatus.RUNNING.code());
        assertEquals(23, UnifiedStatus.PAUSED.code());
        assertEquals(24, UnifiedStatus.STOPPED.code());
        assertEquals(31, UnifiedStatus.PREPARING.code());
        assertEquals(32, UnifiedStatus.PROCESSING.code());
        assertEquals(33, UnifiedStatus.PROCESSED.code());
        assertEquals(34, UnifiedStatus.PENDING.code());
        assertEquals(35, UnifiedStatus.APPROVED.code());
        assertEquals(36, UnifiedStatus.REJECTED.code());
        assertEquals(41, UnifiedStatus.PLACED.code());
        assertEquals(42, UnifiedStatus.SHIPPED.code());
        assertEquals(43, UnifiedStatus.DELIVERED.code());
        assertEquals(44, UnifiedStatus.ACCEPTED.code());
        assertEquals(45, UnifiedStatus.RETURNED.code());
        assertEquals(51, UnifiedStatus.DEVELOPING.code());
        assertEquals(52, UnifiedStatus.TESTING.code());
        assertEquals(53, UnifiedStatus.RELEASED.code());
        assertEquals(54, UnifiedStatus.DEPLOYED.code());
        assertEquals(55, UnifiedStatus.VERIFIED.code());
        assertEquals(61, UnifiedStatus.READ_ONLY.code());
        assertEquals(62, UnifiedStatus.UPDATABLE.code());
        assertEquals(63, UnifiedStatus.INSERTABLE.code());
        assertEquals(64, UnifiedStatus.WRITABLE.code());
        assertEquals(71, UnifiedStatus.FROZEN.code());
        assertEquals(72, UnifiedStatus.FINALIZED.code());
        assertEquals(73, UnifiedStatus.LOCKED.code());
        assertEquals(74, UnifiedStatus.UNLOCKED.code());
        assertEquals(75, UnifiedStatus.AVAILABLE.code());
        assertEquals(76, UnifiedStatus.UNAVAILABLE.code());
        assertEquals(77, UnifiedStatus.ENABLED.code());
        assertEquals(78, UnifiedStatus.DISABLED.code());
    }

    @Test
    public void testFromCode() {
        assertEquals(UnifiedStatus.BLANK, UnifiedStatus.fromCode(0));
        assertEquals(UnifiedStatus.ACTIVE, UnifiedStatus.fromCode(1));
        assertEquals(UnifiedStatus.SUSPENDED, UnifiedStatus.fromCode(2));
        assertEquals(UnifiedStatus.EXPIRED, UnifiedStatus.fromCode(3));
        assertEquals(UnifiedStatus.CANCELED, UnifiedStatus.fromCode(4));
        assertEquals(UnifiedStatus.REFUNDED, UnifiedStatus.fromCode(5));
        assertEquals(UnifiedStatus.COMPLETED, UnifiedStatus.fromCode(6));
        assertEquals(UnifiedStatus.CLOSED, UnifiedStatus.fromCode(7));
        assertEquals(UnifiedStatus.REMOVED, UnifiedStatus.fromCode(8));
        assertEquals(UnifiedStatus.DELETED, UnifiedStatus.fromCode(9));
        assertEquals(UnifiedStatus.CONCLUDED, UnifiedStatus.fromCode(11));
        assertEquals(UnifiedStatus.REVOKED, UnifiedStatus.fromCode(12));
        assertEquals(UnifiedStatus.TERMINATED, UnifiedStatus.fromCode(13));
        assertEquals(UnifiedStatus.RETIRED, UnifiedStatus.fromCode(14));
        assertEquals(UnifiedStatus.DESTROYED, UnifiedStatus.fromCode(15));
        assertEquals(UnifiedStatus.STARTED, UnifiedStatus.fromCode(21));
        assertEquals(UnifiedStatus.RUNNING, UnifiedStatus.fromCode(22));
        assertEquals(UnifiedStatus.PAUSED, UnifiedStatus.fromCode(23));
        assertEquals(UnifiedStatus.STOPPED, UnifiedStatus.fromCode(24));
        assertEquals(UnifiedStatus.PREPARING, UnifiedStatus.fromCode(31));
        assertEquals(UnifiedStatus.PROCESSING, UnifiedStatus.fromCode(32));
        assertEquals(UnifiedStatus.PROCESSED, UnifiedStatus.fromCode(33));
        assertEquals(UnifiedStatus.PENDING, UnifiedStatus.fromCode(34));
        assertEquals(UnifiedStatus.APPROVED, UnifiedStatus.fromCode(35));
        assertEquals(UnifiedStatus.REJECTED, UnifiedStatus.fromCode(36));
        assertEquals(UnifiedStatus.PLACED, UnifiedStatus.fromCode(41));
        assertEquals(UnifiedStatus.SHIPPED, UnifiedStatus.fromCode(42));
        assertEquals(UnifiedStatus.DELIVERED, UnifiedStatus.fromCode(43));
        assertEquals(UnifiedStatus.ACCEPTED, UnifiedStatus.fromCode(44));
        assertEquals(UnifiedStatus.RETURNED, UnifiedStatus.fromCode(45));
        assertEquals(UnifiedStatus.DEVELOPING, UnifiedStatus.fromCode(51));
        assertEquals(UnifiedStatus.TESTING, UnifiedStatus.fromCode(52));
        assertEquals(UnifiedStatus.RELEASED, UnifiedStatus.fromCode(53));
        assertEquals(UnifiedStatus.DEPLOYED, UnifiedStatus.fromCode(54));
        assertEquals(UnifiedStatus.VERIFIED, UnifiedStatus.fromCode(55));
        assertEquals(UnifiedStatus.READ_ONLY, UnifiedStatus.fromCode(61));
        assertEquals(UnifiedStatus.UPDATABLE, UnifiedStatus.fromCode(62));
        assertEquals(UnifiedStatus.INSERTABLE, UnifiedStatus.fromCode(63));
        assertEquals(UnifiedStatus.WRITABLE, UnifiedStatus.fromCode(64));
        assertEquals(UnifiedStatus.FROZEN, UnifiedStatus.fromCode(71));
        assertEquals(UnifiedStatus.FINALIZED, UnifiedStatus.fromCode(72));
        assertEquals(UnifiedStatus.LOCKED, UnifiedStatus.fromCode(73));
        assertEquals(UnifiedStatus.UNLOCKED, UnifiedStatus.fromCode(74));
        assertEquals(UnifiedStatus.AVAILABLE, UnifiedStatus.fromCode(75));
        assertEquals(UnifiedStatus.UNAVAILABLE, UnifiedStatus.fromCode(76));
        assertEquals(UnifiedStatus.ENABLED, UnifiedStatus.fromCode(77));
        assertEquals(UnifiedStatus.DISABLED, UnifiedStatus.fromCode(78));
        assertNull(UnifiedStatus.fromCode(-1));
        assertNull(UnifiedStatus.fromCode(128));
        assertNull(UnifiedStatus.fromCode(999));
        assertNull(UnifiedStatus.fromCode(10));
    }

    @Test
    public void testEnumName() {
        assertEquals("BLANK", UnifiedStatus.BLANK.name());
        assertEquals("ACTIVE", UnifiedStatus.ACTIVE.name());
        assertEquals("PROCESSING", UnifiedStatus.PROCESSING.name());
        assertEquals("READ_ONLY", UnifiedStatus.READ_ONLY.name());
        assertEquals("DESTROYED", UnifiedStatus.DESTROYED.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("BLANK", UnifiedStatus.BLANK.toString());
        assertEquals("ACTIVE", UnifiedStatus.ACTIVE.toString());
        assertEquals("PROCESSING", UnifiedStatus.PROCESSING.toString());
        assertEquals("READ_ONLY", UnifiedStatus.READ_ONLY.toString());
        assertEquals("DESTROYED", UnifiedStatus.DESTROYED.toString());
    }
}
