package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ServiceStatus2025Test extends TestBase {

    @Test
    public void testIntValue_DEFAULT() {
        assertEquals(0, ServiceStatus.DEFAULT.intValue());
    }

    @Test
    public void testIntValue_ACTIVE() {
        assertEquals(1, ServiceStatus.ACTIVE.intValue());
    }

    @Test
    public void testIntValue_SUSPENDED() {
        assertEquals(2, ServiceStatus.SUSPENDED.intValue());
    }

    @Test
    public void testIntValue_EXPIRED() {
        assertEquals(3, ServiceStatus.EXPIRED.intValue());
    }

    @Test
    public void testIntValue_CONCLUDED() {
        assertEquals(4, ServiceStatus.CONCLUDED.intValue());
    }

    @Test
    public void testIntValue_REVOKED() {
        assertEquals(5, ServiceStatus.REVOKED.intValue());
    }

    @Test
    public void testIntValue_REFUNDED() {
        assertEquals(6, ServiceStatus.REFUNDED.intValue());
    }

    @Test
    public void testIntValue_CANCELLED() {
        assertEquals(7, ServiceStatus.CANCELLED.intValue());
    }

    @Test
    public void testValueOf_0() {
        assertEquals(ServiceStatus.DEFAULT, ServiceStatus.valueOf(0));
    }

    @Test
    public void testValueOf_1() {
        assertEquals(ServiceStatus.ACTIVE, ServiceStatus.valueOf(1));
    }

    @Test
    public void testValueOf_2() {
        assertEquals(ServiceStatus.SUSPENDED, ServiceStatus.valueOf(2));
    }

    @Test
    public void testValueOf_3() {
        assertEquals(ServiceStatus.EXPIRED, ServiceStatus.valueOf(3));
    }

    @Test
    public void testValueOf_4() {
        assertEquals(ServiceStatus.CONCLUDED, ServiceStatus.valueOf(4));
    }

    @Test
    public void testValueOf_5() {
        assertEquals(ServiceStatus.REVOKED, ServiceStatus.valueOf(5));
    }

    @Test
    public void testValueOf_6() {
        assertEquals(ServiceStatus.REFUNDED, ServiceStatus.valueOf(6));
    }

    @Test
    public void testValueOf_7() {
        assertEquals(ServiceStatus.CANCELLED, ServiceStatus.valueOf(7));
    }

    @Test
    public void testValueOf_invalid() {
        assertThrows(IllegalArgumentException.class, () -> ServiceStatus.valueOf(10));
    }

    @Test
    public void testValueOf_negative() {
        assertThrows(IllegalArgumentException.class, () -> ServiceStatus.valueOf(-1));
    }

    @Test
    public void testValueOf_byName_DEFAULT() {
        assertEquals(ServiceStatus.DEFAULT, ServiceStatus.valueOf("DEFAULT"));
    }

    @Test
    public void testValueOf_byName_ACTIVE() {
        assertEquals(ServiceStatus.ACTIVE, ServiceStatus.valueOf("ACTIVE"));
    }

    @Test
    public void testValueOf_byName_CANCELLED() {
        assertEquals(ServiceStatus.CANCELLED, ServiceStatus.valueOf("CANCELLED"));
    }

    @Test
    public void testValues() {
        ServiceStatus[] statuses = ServiceStatus.values();
        assertNotNull(statuses);
        assertEquals(8, statuses.length);
    }

    @Test
    public void testValues_order() {
        ServiceStatus[] statuses = ServiceStatus.values();
        assertEquals(ServiceStatus.DEFAULT, statuses[0]);
        assertEquals(ServiceStatus.ACTIVE, statuses[1]);
        assertEquals(ServiceStatus.SUSPENDED, statuses[2]);
        assertEquals(ServiceStatus.EXPIRED, statuses[3]);
        assertEquals(ServiceStatus.CONCLUDED, statuses[4]);
        assertEquals(ServiceStatus.REVOKED, statuses[5]);
        assertEquals(ServiceStatus.REFUNDED, statuses[6]);
        assertEquals(ServiceStatus.CANCELLED, statuses[7]);
    }

    @Test
    public void testIntValue_sequential() {
        ServiceStatus[] statuses = ServiceStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            assertEquals(i, statuses[i].intValue());
        }
    }

    @Test
    public void testIntValue_uniqueness() {
        ServiceStatus[] statuses = ServiceStatus.values();
        for (int i = 0; i < statuses.length; i++) {
            for (int j = i + 1; j < statuses.length; j++) {
                if (statuses[i].intValue() == statuses[j].intValue()) {
                    throw new AssertionError("Duplicate int value: " + statuses[i] + " and " + statuses[j]);
                }
            }
        }
    }

    @Test
    public void testSwitchStatement() {
        ServiceStatus status = ServiceStatus.ACTIVE;
        String result = switch (status) {
            case DEFAULT -> "default";
            case ACTIVE -> "active";
            case SUSPENDED -> "suspended";
            default -> "other";
        };
        assertEquals("active", result);
    }

    @Test
    public void testIntegration_allStatusesRoundTrip() {
        ServiceStatus[] statuses = ServiceStatus.values();
        for (ServiceStatus status : statuses) {
            int value = status.intValue();
            ServiceStatus decoded = ServiceStatus.valueOf(value);
            assertEquals(status, decoded);
        }
    }
}
