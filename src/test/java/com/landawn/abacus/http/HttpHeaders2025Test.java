package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class HttpHeaders2025Test extends TestBase {

    @Test
    public void testCreate() {
        HttpHeaders headers = HttpHeaders.create();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testOfSingleHeader() {
        HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json");
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    public void testOfSingleHeaderWithNull() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of(null, "value"));
    }

    @Test
    public void testOfTwoHeaders() {
        HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json", "Accept", "text/plain");
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));
    }

    @Test
    public void testOfTwoHeadersWithNullName1() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of(null, "value1", "name2", "value2"));
    }

    @Test
    public void testOfTwoHeadersWithNullName2() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of("name1", "value1", null, "value2"));
    }

    @Test
    public void testOfThreeHeaders() {
        HttpHeaders headers = HttpHeaders.of("Header1", "value1", "Header2", "value2", "Header3", "value3");
        assertNotNull(headers);
        assertEquals("value1", headers.get("Header1"));
        assertEquals("value2", headers.get("Header2"));
        assertEquals("value3", headers.get("Header3"));
    }

    @Test
    public void testOfThreeHeadersWithNullName() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of("name1", "value1", null, "value2", "name3", "value3"));
    }

    @Test
    public void testOfMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        map.put("Accept", "text/plain");

        HttpHeaders headers = HttpHeaders.of(map);
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));
    }

    @Test
    public void testOfMapWithNull() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of((Map<String, ?>) null));
    }

    @Test
    public void testCopyOf() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        map.put("Accept", "text/plain");

        HttpHeaders headers = HttpHeaders.copyOf(map);
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));

        // Verify it's a copy by modifying original map
        map.put("Content-Type", "text/xml");
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    public void testCopyOfWithNull() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.copyOf(null));
    }

    @Test
    public void testValueOfString() {
        String value = HttpHeaders.valueOf("test");
        assertEquals("test", value);
    }

    @Test
    public void testValueOfCollection() {
        String value = HttpHeaders.valueOf(Arrays.asList("gzip", "deflate"));
        assertEquals("gzip; deflate", value);
    }

    @Test
    public void testValueOfDate() {
        Date date = new Date(0);
        String value = HttpHeaders.valueOf(date);
        assertNotNull(value);
        assertTrue(value.contains("1970"));
    }

    @Test
    public void testValueOfInstant() {
        Instant instant = Instant.ofEpochMilli(0);
        String value = HttpHeaders.valueOf(instant);
        assertNotNull(value);
        assertTrue(value.contains("1970"));
    }

    @Test
    public void testValueOfOther() {
        Integer number = 42;
        String value = HttpHeaders.valueOf(number);
        assertEquals("42", value);
    }

    @Test
    public void testSetContentType() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentType("application/json");
        assertEquals("application/json", headers.get(HttpHeaders.Names.CONTENT_TYPE));
    }

    @Test
    public void testSetContentEncoding() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentEncoding("gzip");
        assertEquals("gzip", headers.get(HttpHeaders.Names.CONTENT_ENCODING));
    }

    @Test
    public void testSetContentLanguage() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentLanguage("en-US");
        assertEquals("en-US", headers.get(HttpHeaders.Names.CONTENT_LANGUAGE));
    }

    @Test
    public void testSetContentLength() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setContentLength(1024L);
        assertEquals(1024L, headers.get(HttpHeaders.Names.CONTENT_LENGTH));
    }

    @Test
    public void testSetUserAgent() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setUserAgent("MyApp/1.0");
        assertEquals("MyApp/1.0", headers.get(HttpHeaders.Names.USER_AGENT));
    }

    @Test
    public void testSetCookie() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setCookie("sessionId=abc123");
        assertEquals("sessionId=abc123", headers.get(HttpHeaders.Names.COOKIE));
    }

    @Test
    public void testSetAuthorization() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setAuthorization("Bearer token123");
        assertEquals("Bearer token123", headers.get(HttpHeaders.Names.AUTHORIZATION));
    }

    @Test
    public void testSetBasicAuthentication() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setBasicAuthentication("user", "password");
        String auth = (String) headers.get(HttpHeaders.Names.AUTHORIZATION);
        assertNotNull(auth);
        assertTrue(auth.startsWith("Basic "));
    }

    @Test
    public void testSetProxyAuthorization() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setProxyAuthorization("Basic xyz789");
        assertEquals("Basic xyz789", headers.get(HttpHeaders.Names.PROXY_AUTHORIZATION));
    }

    @Test
    public void testSetCacheControl() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setCacheControl("no-cache");
        assertEquals("no-cache", headers.get(HttpHeaders.Names.CACHE_CONTROL));
    }

    @Test
    public void testSetConnection() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setConnection("keep-alive");
        assertEquals("keep-alive", headers.get(HttpHeaders.Names.CONNECTION));
    }

    @Test
    public void testSetHost() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setHost("example.com");
        assertEquals("example.com", headers.get(HttpHeaders.Names.HOST));
    }

    @Test
    public void testSetFrom() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setFrom("user@example.com");
        assertEquals("user@example.com", headers.get(HttpHeaders.Names.FROM));
    }

    @Test
    public void testSetAccept() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setAccept("application/json");
        assertEquals("application/json", headers.get(HttpHeaders.Names.ACCEPT));
    }

    @Test
    public void testSetAcceptEncoding() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setAcceptEncoding("gzip, deflate");
        assertEquals("gzip, deflate", headers.get(HttpHeaders.Names.ACCEPT_ENCODING));
    }

    @Test
    public void testSetAcceptCharset() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setAcceptCharset("utf-8");
        assertEquals("utf-8", headers.get(HttpHeaders.Names.ACCEPT_CHARSET));
    }

    @Test
    public void testSetAcceptLanguage() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setAcceptLanguage("en-US");
        assertEquals("en-US", headers.get(HttpHeaders.Names.ACCEPT_LANGUAGE));
    }

    @Test
    public void testSetAcceptRanges() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setAcceptRanges("bytes");
        assertEquals("bytes", headers.get(HttpHeaders.Names.ACCEPT_RANGES));
    }

    @Test
    public void testSet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("X-Custom-Header", "custom-value");
        assertEquals("custom-value", headers.get("X-Custom-Header"));
    }

    @Test
    public void testSetWithDifferentTypes() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("String-Header", "value");
        headers.set("Integer-Header", 42);
        headers.set("Long-Header", 100L);

        assertEquals("value", headers.get("String-Header"));
        assertEquals(42, headers.get("Integer-Header"));
        assertEquals(100L, headers.get("Long-Header"));
    }

    @Test
    public void testSetAll() {
        Map<String, String> newHeaders = new HashMap<>();
        newHeaders.put("Header1", "value1");
        newHeaders.put("Header2", "value2");

        HttpHeaders headers = HttpHeaders.create();
        headers.setAll(newHeaders);

        assertEquals("value1", headers.get("Header1"));
        assertEquals("value2", headers.get("Header2"));
    }

    @Test
    public void testGet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Test-Header", "test-value");
        assertEquals("test-value", headers.get("Test-Header"));
    }

    @Test
    public void testGetNonExistent() {
        HttpHeaders headers = HttpHeaders.create();
        assertNull(headers.get("Non-Existent"));
    }

    @Test
    public void testRemove() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Test-Header", "test-value");
        Object removed = headers.remove("Test-Header");
        assertEquals("test-value", removed);
        assertNull(headers.get("Test-Header"));
    }

    @Test
    public void testRemoveNonExistent() {
        HttpHeaders headers = HttpHeaders.create();
        Object removed = headers.remove("Non-Existent");
        assertNull(removed);
    }

    @Test
    public void testHeaderNameSet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        Set<String> names = headers.headerNameSet();
        assertNotNull(names);
        assertEquals(2, names.size());
        assertTrue(names.contains("Header1"));
        assertTrue(names.contains("Header2"));
    }

    @Test
    public void testForEach() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        AtomicInteger count = new AtomicInteger(0);
        headers.forEach((name, value) -> count.incrementAndGet());
        assertEquals(2, count.get());
    }

    @Test
    public void testClear() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        headers.clear();
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        HttpHeaders headers = HttpHeaders.create();
        assertTrue(headers.isEmpty());

        headers.set("Header1", "value1");
        assertFalse(headers.isEmpty());
    }

    @Test
    public void testToMap() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        Map<String, Object> map = headers.toMap();
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals("value1", map.get("Header1"));
        assertEquals("value2", map.get("Header2"));
    }

    @Test
    public void testCopy() {
        HttpHeaders original = HttpHeaders.create();
        original.set("Header1", "value1");
        original.set("Header2", "value2");

        HttpHeaders copy = original.copy();
        assertNotNull(copy);
        assertEquals("value1", copy.get("Header1"));
        assertEquals("value2", copy.get("Header2"));

        // Verify it's a true copy
        copy.set("Header1", "modified");
        assertEquals("value1", original.get("Header1"));
        assertEquals("modified", copy.get("Header1"));
    }

    @Test
    public void testHashCode() {
        HttpHeaders headers1 = HttpHeaders.create();
        headers1.set("Header1", "value1");

        HttpHeaders headers2 = HttpHeaders.create();
        headers2.set("Header1", "value1");

        assertEquals(headers1.hashCode(), headers2.hashCode());
    }

    @Test
    public void testEquals() {
        HttpHeaders headers1 = HttpHeaders.create();
        headers1.set("Header1", "value1");

        HttpHeaders headers2 = HttpHeaders.create();
        headers2.set("Header1", "value1");

        HttpHeaders headers3 = HttpHeaders.create();
        headers3.set("Header1", "different");

        assertTrue(headers1.equals(headers2));
        assertFalse(headers1.equals(headers3));
        assertFalse(headers1.equals(null));
        assertFalse(headers1.equals("string"));
    }

    @Test
    public void testToString() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");

        String str = headers.toString();
        assertNotNull(str);
        assertTrue(str.contains("Header1"));
        assertTrue(str.contains("value1"));
    }

    @Test
    public void testMethodChaining() {
        HttpHeaders headers = HttpHeaders.create()
                .setContentType("application/json")
                .setAccept("application/json")
                .setUserAgent("MyApp/1.0")
                .set("X-Custom", "value");

        assertEquals("application/json", headers.get(HttpHeaders.Names.CONTENT_TYPE));
        assertEquals("application/json", headers.get(HttpHeaders.Names.ACCEPT));
        assertEquals("MyApp/1.0", headers.get(HttpHeaders.Names.USER_AGENT));
        assertEquals("value", headers.get("X-Custom"));
    }

    @Test
    public void testHeaderNameConstants() {
        assertEquals("Cache-Control", HttpHeaders.Names.CACHE_CONTROL);
        assertEquals("Content-Length", HttpHeaders.Names.CONTENT_LENGTH);
        assertEquals("Content-Type", HttpHeaders.Names.CONTENT_TYPE);
        assertEquals("Accept", HttpHeaders.Names.ACCEPT);
        assertEquals("Authorization", HttpHeaders.Names.AUTHORIZATION);
        assertEquals("User-Agent", HttpHeaders.Names.USER_AGENT);
    }

    @Test
    public void testHeaderValueConstants() {
        assertEquals("application/x-www-form-urlencoded", HttpHeaders.Values.APPLICATION_URL_ENCODED);
        assertEquals("application/xml", HttpHeaders.Values.APPLICATION_XML);
        assertEquals("application/json", HttpHeaders.Values.APPLICATION_JSON);
        assertEquals("text/html", HttpHeaders.Values.TEXT_HTML);
        assertEquals("utf-8", HttpHeaders.Values.UTF_8);
    }

    @Test
    public void testReferrerPolicyValueConstants() {
        assertEquals("no-referrer", HttpHeaders.ReferrerPolicyValues.NO_REFERRER);
        assertEquals("same-origin", HttpHeaders.ReferrerPolicyValues.SAME_ORIGIN);
        assertEquals("origin", HttpHeaders.ReferrerPolicyValues.ORIGIN);
        assertEquals("unsafe-url", HttpHeaders.ReferrerPolicyValues.UNSAFE_URL);
    }
}
