package com.landawn.abacus.bugfix;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Node;

import com.landawn.abacus.http.HttpHeaders;
import com.landawn.abacus.http.HttpSettings;
import com.landawn.abacus.http.v2.HttpRequest;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.XmlUtil;

public class NullArgumentValidationReviewTest extends com.landawn.abacus.TestBase {
    @Test
    public void headerArgumentsUseIllegalArgumentExceptionAndPreserveBackingMapPolicy() {
        final HttpHeaders headers = HttpHeaders.of("Existing", "value");
        assertThrowsExactly(IllegalArgumentException.class, () -> headers.setAll(null));
        assertEquals("value", headers.get("Existing"));
        assertThrowsExactly(IllegalArgumentException.class, () -> new HttpSettings().headers((Map<String, ?>) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpRequest.url("http://localhost/").header(null, "value"));
        assertThrowsExactly(IllegalArgumentException.class, () -> com.landawn.abacus.http.HttpUtil.HttpDate.format(null));
        assertEquals("Thu, 01 Jan 1970 00:00:00 GMT", com.landawn.abacus.http.HttpUtil.HttpDate.format(new java.util.Date(0)));
        assertDoesNotThrow(() -> headers.set("Nullable", null));
        final HttpHeaders concurrentHeaders = HttpHeaders.wrap(new ConcurrentHashMap<>());
        assertThrowsExactly(NullPointerException.class, () -> concurrentHeaders.set("Nullable", null));
        assertThrowsExactly(NullPointerException.class, () -> concurrentHeaders.setAll(java.util.Collections.singletonMap("Nullable", null)));
    }

    @Test
    public void urlFactoriesRejectNullConsistentlyWithoutExecutingRequests() throws Exception {
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpRequest.create((URL) null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpRequest.url((URL) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpRequest.url((URL) null, 100, 100));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpRequest.url((String) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpRequest.url((URI) null));
        final URL url = URI.create("http://localhost/").toURL();
        assertDoesNotThrow(() -> HttpRequest.create(url, null));
        assertDoesNotThrow(() -> HttpRequest.url(url));
        assertDoesNotThrow(() -> HttpRequest.url(url, 100, 100));
    }

    @Test
    public void xmlNodeOverloadsValidateSourceAndTargetBeforeParsing() throws Exception {
        final Node node = XmlUtil.createDOMParser().newDocument().createElement("value");
        for (final XmlParser parser : new XmlParser[] { ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser() }) {
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize((Node) null, String.class));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize((Node) null, Type.of(String.class)));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize((Node) null, null, String.class));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize((Node) null, null, Type.of(String.class)));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize((Node) null, null, Map.of("value", Type.of(String.class))));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize(node, (Type<String>) null));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize(node, null, (Type<String>) null));
            assertThrowsExactly(IllegalArgumentException.class, () -> parser.deserialize(node, (Class<String>) null));
        }
    }
}
