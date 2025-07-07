package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class HttpMethod100Test extends TestBase {

    @Test
    public void testEnumValues() {
        HttpMethod[] values = HttpMethod.values();
        assertEquals(9, values.length);
        
        // Verify all enum constants exist
        assertNotNull(HttpMethod.GET);
        assertNotNull(HttpMethod.POST);
        assertNotNull(HttpMethod.PUT);
        assertNotNull(HttpMethod.DELETE);
        assertNotNull(HttpMethod.HEAD);
        assertNotNull(HttpMethod.OPTIONS);
        assertNotNull(HttpMethod.TRACE);
        assertNotNull(HttpMethod.CONNECT);
        assertNotNull(HttpMethod.PATCH);
    }

    @Test
    public void testValueOf() {
        assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"));
        assertEquals(HttpMethod.POST, HttpMethod.valueOf("POST"));
        assertEquals(HttpMethod.PUT, HttpMethod.valueOf("PUT"));
        assertEquals(HttpMethod.DELETE, HttpMethod.valueOf("DELETE"));
        assertEquals(HttpMethod.HEAD, HttpMethod.valueOf("HEAD"));
        assertEquals(HttpMethod.OPTIONS, HttpMethod.valueOf("OPTIONS"));
        assertEquals(HttpMethod.TRACE, HttpMethod.valueOf("TRACE"));
        assertEquals(HttpMethod.CONNECT, HttpMethod.valueOf("CONNECT"));
        assertEquals(HttpMethod.PATCH, HttpMethod.valueOf("PATCH"));
    }

    @Test
    public void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () -> HttpMethod.valueOf("INVALID"));
        assertThrows(IllegalArgumentException.class, () -> HttpMethod.valueOf("get")); // lowercase
        assertThrows(NullPointerException.class, () -> HttpMethod.valueOf(null));
    }

    @Test
    public void testName() {
        assertEquals("GET", HttpMethod.GET.name());
        assertEquals("POST", HttpMethod.POST.name());
        assertEquals("PUT", HttpMethod.PUT.name());
        assertEquals("DELETE", HttpMethod.DELETE.name());
        assertEquals("HEAD", HttpMethod.HEAD.name());
        assertEquals("OPTIONS", HttpMethod.OPTIONS.name());
        assertEquals("TRACE", HttpMethod.TRACE.name());
        assertEquals("CONNECT", HttpMethod.CONNECT.name());
        assertEquals("PATCH", HttpMethod.PATCH.name());
    }

    @Test
    public void testOrdinal() {
        assertEquals(0, HttpMethod.GET.ordinal());
        assertEquals(1, HttpMethod.POST.ordinal());
        assertEquals(2, HttpMethod.PUT.ordinal());
        assertEquals(3, HttpMethod.DELETE.ordinal());
        assertEquals(4, HttpMethod.HEAD.ordinal());
        assertEquals(5, HttpMethod.OPTIONS.ordinal());
        assertEquals(6, HttpMethod.TRACE.ordinal());
        assertEquals(7, HttpMethod.CONNECT.ordinal());
        assertEquals(8, HttpMethod.PATCH.ordinal());
    }
}
