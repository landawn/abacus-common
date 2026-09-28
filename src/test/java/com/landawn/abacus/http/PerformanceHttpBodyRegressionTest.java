package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

@Tag("unit")
class PerformanceHttpBodyRegressionTest {
    @Test
    void ownedBodyAvoidsHandoffCopyButPublicAccessorsRemainDefensive() throws Exception {
        byte[] body = { 1, 2, 3 };
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Test", new ArrayList<>(List.of("original")));
        HttpResponse response = HttpResponse.withOwnedBody("url", 1, 2, 200, "OK", headers, body, ContentFormat.NONE, null);
        Field field = HttpResponse.class.getDeclaredField("body");
        field.setAccessible(true);
        assertSame(body, field.get(response));
        response.body()[0] = 9;
        response.body(byte[].class)[1] = 9;
        assertArrayEquals(new byte[] { 1, 2, 3 }, response.body());
        headers.get("X-Test").set(0, "changed");
        assertEquals(List.of("original"), response.headers().get("X-Test"));

        HttpResponse copied = new HttpResponse("url", 1, 2, 200, "OK", headers, body, ContentFormat.NONE, null);
        assertNotSame(body, field.get(copied));
        body[0] = 8;
        assertArrayEquals(new byte[] { 1, 2, 3 }, copied.body());
    }
}
