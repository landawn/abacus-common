package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class DigestUnicodeTest extends TestBase {
    private static List<Method> stringDigests() {
        return Arrays.stream(DigestUtil.class.getMethods())
                .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class)
                .filter(m -> m.getReturnType() == byte[].class || m.getReturnType() == String.class)
                .toList();
    }

    @Test
    void everyStringDigestAndAliasRejectsMalformedTextAndKeepsValidBytes() throws Exception {
        final List<Method> methods = stringDigests();
        assertEquals(28, methods.size());
        for (final Method method : methods) {
            for (final String malformed : List.of("\uD800", "\uDC00", "a\uD800b", "a\uDC00b", "\uDC00\uD800", "\uD800\uD800", "\uD83D\uDE00\uD800")) {
                final var failure = assertThrows(InvocationTargetException.class, () -> method.invoke(null, malformed));
                assertInstanceOf(IllegalArgumentException.class, failure.getCause(), method.getName());
            }
            assertInstanceOf(IllegalArgumentException.class,
                    assertThrows(InvocationTargetException.class, () -> method.invoke(null, (Object) null)).getCause());
            final Method binary = DigestUtil.class.getMethod(method.getName(), byte[].class);
            for (final String valid : List.of("", "ASCII?", "\u03B1", "\uD83D\uDE00", "\uDBFF\uDFFF", "e\u0301")) {
                final Object expected = binary.invoke(null, valid.getBytes(StandardCharsets.UTF_8));
                final Object actual = method.invoke(null, valid);
                if (expected instanceof byte[] bytes) {
                    assertArrayEquals(bytes, (byte[]) actual);
                } else {
                    assertEquals(expected, actual);
                }
            }
        }
    }

    @Test
    void rejectedUpdatesPreserveDigestStateForEveryLoneSurrogate() {
        for (int unit = Character.MIN_SURROGATE; unit <= Character.MAX_SURROGATE; unit++) {
            final var digest = DigestUtil.getSha256Digest();
            digest.update((byte) 7);
            final String malformed = "a" + (char) unit + "b";
            assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(digest, malformed));
            assertSame(digest, DigestUtil.updateDigest(digest, "\uD83D\uDE00"));
            final var expected = DigestUtil.getSha256Digest();
            expected.update((byte) 7);
            assertArrayEquals(expected.digest("\uD83D\uDE00".getBytes(StandardCharsets.UTF_8)), digest.digest());
        }
        assertThrows(IllegalArgumentException.class, () -> DigestUtil.updateDigest(null, "\uD800"));
    }

    @Test
    void passwordVerificationRejectsEncodingCollisionsAndRecoversConcurrently() throws Exception {
        final Password password = new Password("SHA-256");
        final String ordinary = password.digest("a?b");
        for (final String malformed : List.of("a\uD800b", "a\uDC00b", "\uDC00\uD800")) {
            assertThrows(IllegalArgumentException.class, () -> password.digest(malformed));
            assertThrows(IllegalArgumentException.class, () -> password.isEqual(malformed, ordinary));
            assertEquals(ordinary, password.digest("a?b"));
            assertFalse(password.isEqual(malformed, null));
        }
        assertNull(password.digest(null));
        assertTrue(password.isEqual(null, null));
        assertFalse(password.isEqual(null, ordinary));
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> password.digest("\uD83D\uDE00"));
            final var second = executor.submit(() -> {
                assertThrows(IllegalArgumentException.class, () -> password.digest("\uD800"));
                return password.digest("\uD83D\uDE00");
            });
            assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void websocketSecurityFieldsAreValidatedIndependently() {
        for (int position = 0; position < 3; position++) {
            for (final String malformed : List.of("\uD800", "\uDC00", "\uDC00\uD800")) {
                final String[] values = { "nonce", "created", "password" };
                values[position] = malformed;
                assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(values[0], values[1], values[2]));
                assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest(values[0], values[1], values[2], "SHA-256"));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest("\uD800", "\uDC00", ""));
        for (final String value : List.of("", "?", "\u03B1", "\uD83D\uDE00")) {
            final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            assertEquals(WSSecurityUtil.computePasswordDigest(bytes, bytes, bytes), WSSecurityUtil.computePasswordDigest(value, value, value));
            assertEquals(WSSecurityUtil.computePasswordDigest(bytes, bytes, bytes, "SHA-256"),
                    WSSecurityUtil.computePasswordDigest(value, value, value, "SHA-256"));
        }
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest((String) null, "", ""));
        assertThrows(IllegalArgumentException.class, () -> WSSecurityUtil.computePasswordDigest("", "", "", null));
    }
}
