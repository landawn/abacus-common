package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PatternTypeTest extends TestBase {

    private PatternType patternType;

    @BeforeEach
    public void setUp() {
        patternType = (PatternType) createType("Pattern");
    }

    @Test
    public void testClazz() {
        assertEquals(Pattern.class, patternType.javaType());
    }

    @Test
    public void testStringOfWithSimplePattern() {
        Pattern pattern = Pattern.compile("test");
        String result = patternType.stringOf(pattern);
        assertEquals("test", result);
    }

    @Test
    public void testStringOfWithComplexPattern() {
        Pattern pattern = Pattern.compile("[a-zA-Z]+\\d+");
        String result = patternType.stringOf(pattern);
        assertEquals("[a-zA-Z]+\\d+", result);
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(patternType.stringOf(null));
    }

    @Test
    public void testStringOfWithFlags() {
        Pattern pattern = Pattern.compile("test", Pattern.CASE_INSENSITIVE);
        String result = patternType.stringOf(pattern);
        assertNotNull(result);
        assertTrue(result.contains("test"));
    }

    @Test
    public void testRoundTrip() {
        String originalRegex = "\\d{4}-\\d{2}-\\d{2}";
        Pattern pattern1 = patternType.valueOf(originalRegex);
        String stringRepresentation = patternType.stringOf(pattern1);
        Pattern pattern2 = patternType.valueOf(stringRepresentation);

        assertEquals(pattern1.pattern(), pattern2.pattern());
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(patternType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        Pattern pattern = patternType.valueOf("");

        assertNotNull(pattern);
        assertEquals("", pattern.pattern());
        assertTrue(pattern.matcher("").matches());
    }

    @Test
    public void testEmptyPatternRoundTrip() {
        Pattern original = Pattern.compile("");
        Pattern restored = patternType.valueOf(patternType.stringOf(original));

        assertNotNull(restored);
        assertEquals(original.pattern(), restored.pattern());
    }

    @Test
    public void testValueOfWithSimplePattern() {
        Pattern pattern = patternType.valueOf("abc");
        assertNotNull(pattern);
        assertEquals("abc", pattern.pattern());
    }

    @Test
    public void testValueOfWithComplexPattern() {
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = patternType.valueOf(regex);
        assertNotNull(pattern);
        assertEquals(regex, pattern.pattern());
    }

    @Test
    public void testValueOfWithSpecialCharacters() {
        String regex = "\\d{3}-\\d{3}-\\d{4}";
        Pattern pattern = patternType.valueOf(regex);
        assertNotNull(pattern);
        assertEquals(regex, pattern.pattern());
    }

    @Test
    public void testVariousPatterns() {
        String[] patterns = { ".*", "^start", "end$", "(group1|group2)", "a{2,5}", "[^abc]", "\\s+", "(?i)case", };

        for (String regex : patterns) {
            Pattern pattern = patternType.valueOf(regex);
            assertNotNull(pattern);
            assertEquals(regex, pattern.pattern());
        }
    }

    @Test
    public void testValueOfWithInvalidPattern() {
        assertThrows(PatternSyntaxException.class, () -> {
            patternType.valueOf("[");
        });
    }

    @Test
    public void testPattercountMatchBetweening() {
        Pattern emailPattern = patternType.valueOf("\\w+@\\w+\\.\\w+");
        assertNotNull(emailPattern);
        assertTrue(emailPattern.matcher("test@example.com").find());
        assertFalse(emailPattern.matcher("invalid-email").find());
    }

    @Test
    public void testName() {
        assertEquals("Pattern", patternType.name());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(patternType.isSerializable());
    }

    // ---- review fixes 2026-09-06, T2-10 (WONTFIX, documented): identity equals and the package-wide isImmutable default ----

    @Test
    public void reviewFixes20260906_equalsIsIdentityAndIsImmutableIsTheInheritedDefault() {
        final Pattern p = Pattern.compile("a+b", Pattern.CASE_INSENSITIVE);
        final Pattern roundTrip = patternType.valueOf(patternType.stringOf(p));

        assertEquals(p.pattern(), roundTrip.pattern());
        // Pattern does not override equals(): the round trip is a different instance, hence not equal.
        assertFalse(patternType.equals(p, roundTrip));
        assertTrue(patternType.equals(p, p));
        assertFalse(patternType.equals(Pattern.compile("x"), Pattern.compile("x")));
        assertFalse(patternType.isImmutable());
    }
}
