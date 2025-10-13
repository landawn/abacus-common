package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class HttpHeaders100Test extends TestBase {

    @Test
    public void testCreate() {
        HttpHeaders headers = HttpHeaders.create();
        assertNotNull(headers);
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testOfWithSingleHeader() {
        HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json");
        assertNotNull(headers);
        assertEquals("application/json", headers.get("Content-Type"));
    }

    @Test
    public void testOfWithSingleHeaderNullName() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of(null, "value"));
    }

    @Test
    public void testOfWithTwoHeaders() {
        HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json", "Accept", "text/plain");
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));
    }

    @Test
    public void testOfWithTwoHeadersNullNames() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of(null, "value1", "name2", "value2"));
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of("name1", "value1", null, "value2"));
    }

    @Test
    public void testOfWithThreeHeaders() {
        HttpHeaders headers = HttpHeaders.of("Content-Type", "application/json", "Accept", "text/plain", "Authorization", "Bearer token");
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));
        assertEquals("Bearer token", headers.get("Authorization"));
    }

    @Test
    public void testOfWithThreeHeadersNullNames() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of(null, "value1", "name2", "value2", "name3", "value3"));
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of("name1", "value1", null, "value2", "name3", "value3"));
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of("name1", "value1", "name2", "value2", null, "value3"));
    }

    @Test
    public void testOfWithMap() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        map.put("Accept", "text/plain");

        HttpHeaders headers = HttpHeaders.of(map);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));
    }

    @Test
    public void testOfWithMapNull() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.of((Map<String, ?>) null));
    }

    @Test
    public void testCopyOf() {
        Map<String, String> map = new HashMap<>();
        map.put("Content-Type", "application/json");
        map.put("Accept", "text/plain");

        HttpHeaders headers = HttpHeaders.copyOf(map);
        assertEquals("application/json", headers.get("Content-Type"));
        assertEquals("text/plain", headers.get("Accept"));

        map.put("New-Header", "value");
        assertNull(headers.get("New-Header"));
    }

    @Test
    public void testCopyOfNull() {
        assertThrows(IllegalArgumentException.class, () -> HttpHeaders.copyOf(null));
    }

    @Test
    public void testValueOfString() {
        assertEquals("test", HttpHeaders.valueOf("test"));
    }

    @Test
    public void testValueOfCollection() {
        List<String> values = Arrays.asList("gzip", "deflate", "br");
        assertEquals("gzip; deflate; br", HttpHeaders.valueOf(values));
    }

    @Test
    public void testValueOfDate() {
        Date date = new Date();
        String formatted = HttpHeaders.valueOf(date);
        assertNotNull(formatted);
        assertTrue(formatted.contains("GMT"));
    }

    @Test
    public void testValueOfInstant() {
        Instant instant = Instant.now();
        String formatted = HttpHeaders.valueOf(instant);
        assertNotNull(formatted);
        assertTrue(formatted.contains("GMT"));
    }

    @Test
    public void testValueOfObject() {
        Integer num = 42;
        assertEquals("42", HttpHeaders.valueOf(num));
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
        headers.setBasicAuthentication("user", "pass");
        String authHeader = (String) headers.get(HttpHeaders.Names.AUTHORIZATION);
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Basic "));
    }

    @Test
    public void testSetProxyAuthorization() {
        HttpHeaders headers = HttpHeaders.create();
        headers.setProxyAuthorization("Bearer proxy-token");
        assertEquals("Bearer proxy-token", headers.get(HttpHeaders.Names.PROXY_AUTHORIZATION));
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
        headers.setHost("api.example.com");
        assertEquals("api.example.com", headers.get(HttpHeaders.Names.HOST));
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
        headers.setAcceptLanguage("en-US,en;q=0.9");
        assertEquals("en-US,en;q=0.9", headers.get(HttpHeaders.Names.ACCEPT_LANGUAGE));
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
    public void testSetAll() {
        HttpHeaders headers = HttpHeaders.create();
        Map<String, String> map = new HashMap<>();
        map.put("Header1", "value1");
        map.put("Header2", "value2");

        headers.setAll(map);
        assertEquals("value1", headers.get("Header1"));
        assertEquals("value2", headers.get("Header2"));
    }

    @Test
    public void testGet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Test-Header", "test-value");
        assertEquals("test-value", headers.get("Test-Header"));
        assertNull(headers.get("Non-Existent"));
    }

    @Test
    public void testRemove() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Test-Header", "test-value");
        assertEquals("test-value", headers.remove("Test-Header"));
        assertNull(headers.get("Test-Header"));
    }

    @Test
    public void testHeaderNameSet() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        Set<String> names = headers.headerNameSet();
        assertEquals(2, names.size());
        assertTrue(names.contains("Header1"));
        assertTrue(names.contains("Header2"));
    }

    @Test
    public void testForEach() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        Map<String, Object> collected = new HashMap<>();
        headers.forEach((name, value) -> collected.put(name, value));

        assertEquals(2, collected.size());
        assertEquals("value1", collected.get("Header1"));
        assertEquals("value2", collected.get("Header2"));
    }

    @Test
    public void testClear() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        assertFalse(headers.isEmpty());
        headers.clear();
        assertTrue(headers.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        HttpHeaders headers = HttpHeaders.create();
        assertTrue(headers.isEmpty());

        headers.set("Header", "value");
        assertFalse(headers.isEmpty());
    }

    @Test
    public void testToMap() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        Map<String, Object> map = headers.toMap();
        assertEquals(2, map.size());
        assertEquals("value1", map.get("Header1"));
        assertEquals("value2", map.get("Header2"));
    }

    @Test
    public void testCopy() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        HttpHeaders copy = headers.copy();
        assertEquals("value1", copy.get("Header1"));
        assertEquals("value2", copy.get("Header2"));

        headers.set("Header3", "value3");
        assertNull(copy.get("Header3"));
    }

    @Test
    public void testHashCode() {
        HttpHeaders headers1 = HttpHeaders.create();
        headers1.set("Header", "value");

        HttpHeaders headers2 = HttpHeaders.create();
        headers2.set("Header", "value");

        assertEquals(headers1.hashCode(), headers2.hashCode());
    }

    @Test
    public void testEquals() {
        HttpHeaders headers1 = HttpHeaders.create();
        headers1.set("Header", "value");

        HttpHeaders headers2 = HttpHeaders.create();
        headers2.set("Header", "value");

        assertEquals(headers1, headers2);

        headers2.set("Another", "value");
        assertNotEquals(headers1, headers2);

        assertNotEquals(headers1, "not a headers object");
    }

    @Test
    public void testToString() {
        HttpHeaders headers = HttpHeaders.create();
        headers.set("Header1", "value1");
        headers.set("Header2", "value2");

        String str = headers.toString();
        assertNotNull(str);
        assertTrue(str.contains("Header1"));
        assertTrue(str.contains("value1"));
        assertTrue(str.contains("Header2"));
        assertTrue(str.contains("value2"));
    }

    @Test
    public void testNamesConstants() {
        assertEquals("Content-Type", HttpHeaders.Names.CONTENT_TYPE);
        assertEquals("Content-Length", HttpHeaders.Names.CONTENT_LENGTH);
        assertEquals("Accept", HttpHeaders.Names.ACCEPT);
        assertEquals("Authorization", HttpHeaders.Names.AUTHORIZATION);
        assertEquals("User-Agent", HttpHeaders.Names.USER_AGENT);
    }

    @Test
    public void testValuesConstants() {
        assertEquals("application/x-www-form-urlencoded", HttpHeaders.Values.APPLICATION_URL_ENCODED);
        assertEquals("application/xml", HttpHeaders.Values.APPLICATION_XML);
        assertEquals("application/json", HttpHeaders.Values.APPLICATION_JSON);
        assertEquals("application/kryo", HttpHeaders.Values.APPLICATION_KRYO);
        assertEquals("text/html", HttpHeaders.Values.TEXT_HTML);
        assertEquals("text/json", HttpHeaders.Values.TEXT_JSON);
        assertEquals("text/xml", HttpHeaders.Values.TEXT_XML);
        assertEquals("image/gif", HttpHeaders.Values.IMAGE_GIF);
        assertEquals("image/jpg", HttpHeaders.Values.IMAGE_JPG);
        assertEquals("utf-8", HttpHeaders.Values.UTF_8);
    }

    @Test
    public void testReferrerPolicyValuesConstants() {
        assertEquals("no-referrer", HttpHeaders.ReferrerPolicyValues.NO_REFERRER);
        assertEquals("no-referrer-when-downgrade", HttpHeaders.ReferrerPolicyValues.NO_REFERRER_WHEN_DOWNGRADE);
        assertEquals("same-origin", HttpHeaders.ReferrerPolicyValues.SAME_ORIGIN);
        assertEquals("origin", HttpHeaders.ReferrerPolicyValues.ORIGIN);
        assertEquals("strict-origin", HttpHeaders.ReferrerPolicyValues.STRICT_ORIGIN);
        assertEquals("origin-when-cross-origin", HttpHeaders.ReferrerPolicyValues.ORIGIN_WHEN_CROSS_ORIGIN);
        assertEquals("strict-origin-when-cross-origin", HttpHeaders.ReferrerPolicyValues.STRICT_ORIGIN_WHEN_CROSS_ORIGIN);
        assertEquals("unsafe-url", HttpHeaders.ReferrerPolicyValues.UNSAFE_URL);
    }
}
