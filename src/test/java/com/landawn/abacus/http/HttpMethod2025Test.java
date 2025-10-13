package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class HttpMethod2025Test extends TestBase {

    @Test
    public void testEnumValues() {
        HttpMethod[] values = HttpMethod.values();
        assertNotNull(values);
        assertEquals(9, values.length);
    }

    @Test
    public void testValueOfGET() {
        assertEquals(HttpMethod.GET, HttpMethod.valueOf("GET"));
    }

    @Test
    public void testValueOfPOST() {
        assertEquals(HttpMethod.POST, HttpMethod.valueOf("POST"));
    }

    @Test
    public void testValueOfPUT() {
        assertEquals(HttpMethod.PUT, HttpMethod.valueOf("PUT"));
    }

    @Test
    public void testValueOfDELETE() {
        assertEquals(HttpMethod.DELETE, HttpMethod.valueOf("DELETE"));
    }

    @Test
    public void testValueOfHEAD() {
        assertEquals(HttpMethod.HEAD, HttpMethod.valueOf("HEAD"));
    }

    @Test
    public void testValueOfOPTIONS() {
        assertEquals(HttpMethod.OPTIONS, HttpMethod.valueOf("OPTIONS"));
    }

    @Test
    public void testValueOfTRACE() {
        assertEquals(HttpMethod.TRACE, HttpMethod.valueOf("TRACE"));
    }

    @Test
    public void testValueOfCONNECT() {
        assertEquals(HttpMethod.CONNECT, HttpMethod.valueOf("CONNECT"));
    }

    @Test
    public void testValueOfPATCH() {
        assertEquals(HttpMethod.PATCH, HttpMethod.valueOf("PATCH"));
    }

    @Test
    public void testNameGET() {
        assertEquals("GET", HttpMethod.GET.name());
    }

    @Test
    public void testNamePOST() {
        assertEquals("POST", HttpMethod.POST.name());
    }

    @Test
    public void testNamePUT() {
        assertEquals("PUT", HttpMethod.PUT.name());
    }

    @Test
    public void testNameDELETE() {
        assertEquals("DELETE", HttpMethod.DELETE.name());
    }

    @Test
    public void testNameHEAD() {
        assertEquals("HEAD", HttpMethod.HEAD.name());
    }

    @Test
    public void testNameOPTIONS() {
        assertEquals("OPTIONS", HttpMethod.OPTIONS.name());
    }

    @Test
    public void testNameTRACE() {
        assertEquals("TRACE", HttpMethod.TRACE.name());
    }

    @Test
    public void testNameCONNECT() {
        assertEquals("CONNECT", HttpMethod.CONNECT.name());
    }

    @Test
    public void testNamePATCH() {
        assertEquals("PATCH", HttpMethod.PATCH.name());
    }

    @Test
    public void testToStringGET() {
        assertEquals("GET", HttpMethod.GET.toString());
    }

    @Test
    public void testToStringPOST() {
        assertEquals("POST", HttpMethod.POST.toString());
    }

    @Test
    public void testToStringPUT() {
        assertEquals("PUT", HttpMethod.PUT.toString());
    }

    @Test
    public void testToStringDELETE() {
        assertEquals("DELETE", HttpMethod.DELETE.toString());
    }

    @Test
    public void testOrdinalGET() {
        assertEquals(0, HttpMethod.GET.ordinal());
    }

    @Test
    public void testOrdinalPOST() {
        assertEquals(1, HttpMethod.POST.ordinal());
    }

    @Test
    public void testOrdinalPUT() {
        assertEquals(2, HttpMethod.PUT.ordinal());
    }

    @Test
    public void testOrdinalDELETE() {
        assertEquals(3, HttpMethod.DELETE.ordinal());
    }

    @Test
    public void testOrdinalHEAD() {
        assertEquals(4, HttpMethod.HEAD.ordinal());
    }

    @Test
    public void testOrdinalOPTIONS() {
        assertEquals(5, HttpMethod.OPTIONS.ordinal());
    }

    @Test
    public void testOrdinalTRACE() {
        assertEquals(6, HttpMethod.TRACE.ordinal());
    }

    @Test
    public void testOrdinalCONNECT() {
        assertEquals(7, HttpMethod.CONNECT.ordinal());
    }

    @Test
    public void testOrdinalPATCH() {
        assertEquals(8, HttpMethod.PATCH.ordinal());
    }
}
