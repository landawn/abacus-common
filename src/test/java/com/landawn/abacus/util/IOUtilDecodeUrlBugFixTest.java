package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the infinite loop bug fix in IOUtil.decodeUrl.
 *
 * Bug: When decodeUrl encountered a malformed percent-encoded sequence
 * (e.g., "%zz" or truncated "%a"), Integer.parseInt would throw an exception.
 * Since 'i' was only incremented after a successful parse, the for loop
 * would retry the same '%' character indefinitely, causing an infinite loop.
 *
 * Fix: In the catch block, if 'i' hasn't advanced (still at the '%'),
 * append '%' literally and increment 'i' to prevent re-processing.
 */
@Tag("bugfix")
public class IOUtilDecodeUrlBugFixTest extends TestBase {

    private static String invokeDecodeUrl(String url) throws Exception {
        final Method decodeUrl = IOUtil.class.getDeclaredMethod("decodeUrl", String.class);
        decodeUrl.setAccessible(true);
        return (String) decodeUrl.invoke(null, url);
    }

    @Test
    public void testDecodeUrl_truncatedPercentEncoding_noInfiniteLoop() throws Exception {
        // "%a" at end of string - only 1 hex char instead of 2
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            String result = invokeDecodeUrl("hello%a");
            assertEquals("hello%a", result);
        });
    }

    @Test
    public void testDecodeUrl_validPercentEncoding_stillWorks() throws Exception {
        // Valid %20 = space
        String result = invokeDecodeUrl("hello%20world");
        assertEquals("hello world", result);
    }

    @Test
    public void testDecodeUrl_null_passthrough() throws Exception {
        String result = invokeDecodeUrl(null);
        assertNull(result);
    }

    @Test
    public void testDecodeUrl_mixedValidAndInvalid() throws Exception {
        // Valid %20 followed by invalid %zz
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            String result = invokeDecodeUrl("a%20b%zzc");
            assertEquals("a b%zzc", result);
        });
    }
}
