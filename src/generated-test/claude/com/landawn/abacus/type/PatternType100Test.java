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

public class PatternType100Test extends TestBase {

    private PatternType patternType;

    @BeforeEach
    public void setUp() {
        patternType = (PatternType) createType("Pattern");
    }

    @Test
    public void testClazz() {
        assertEquals(Pattern.class, patternType.clazz());
    }

    @Test
    public void testStringOfWithNull() {
        assertNull(patternType.stringOf(null));
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
    public void testStringOfWithFlags() {
        Pattern pattern = Pattern.compile("test", Pattern.CASE_INSENSITIVE);
        String result = patternType.stringOf(pattern);
        assertNotNull(result);
        // The pattern string includes flag information in its toString()
        assertTrue(result.contains("test"));
    }

    @Test
    public void testValueOfWithNull() {
        assertNull(patternType.valueOf(null));
    }

    @Test
    public void testValueOfWithEmptyString() {
        assertNull(patternType.valueOf(""));
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
        String regex = "\\d{3}-\\d{3}-\\d{4}"; // Phone number pattern
        Pattern pattern = patternType.valueOf(regex);
        assertNotNull(pattern);
        assertEquals(regex, pattern.pattern());
    }

    @Test
    public void testValueOfWithInvalidPattern() {
        // Invalid regex pattern should throw exception
        assertThrows(PatternSyntaxException.class, () -> {
            patternType.valueOf("[");
        });
    }

    @Test
    public void testPatternMatching() {
        // Test that created patterns work correctly
        Pattern emailPattern = patternType.valueOf("\\w+@\\w+\\.\\w+");
        assertNotNull(emailPattern);
        assertTrue(emailPattern.matcher("test@example.com").find());
        assertFalse(emailPattern.matcher("invalid-email").find());
    }

    @Test
    public void testRoundTrip() {
        // Test that stringOf and valueOf are inverse operations
        String originalRegex = "\\d{4}-\\d{2}-\\d{2}"; // Date pattern
        Pattern pattern1 = patternType.valueOf(originalRegex);
        String stringRepresentation = patternType.stringOf(pattern1);
        Pattern pattern2 = patternType.valueOf(stringRepresentation);

        assertEquals(pattern1.pattern(), pattern2.pattern());
    }

    @Test
    public void testName() {
        assertEquals("Pattern", patternType.name());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(patternType.isSerializable());
    }

    @Test
    public void testVariousPatterns() {
        // Test various common regex patterns
        String[] patterns = { ".*", // Match all
                "^start", // Start anchor
                "end$", // End anchor
                "(group1|group2)", // Groups
                "a{2,5}", // Quantifiers
                "[^abc]", // Negated character class
                "\\s+", // Whitespace
                "(?i)case", // Inline flags
        };

        for (String regex : patterns) {
            Pattern pattern = patternType.valueOf(regex);
            assertNotNull(pattern);
            assertEquals(regex, pattern.pattern());
        }
    }
}
