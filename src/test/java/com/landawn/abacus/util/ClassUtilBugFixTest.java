package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for bug fixes in ClassUtil.
 */
@Tag("bugfix")
public class ClassUtilBugFixTest extends TestBase {

    /**
     * Bug fix: ClassUtil.formatParameterizedTypeName had an off-by-one error
     * in the substring comparison for detecting duplicated type name segments
     * before '$' in inner class names.
     *
     * The original code used tmp.substring(tmp.length() / 2 + 1) which skipped
     * a character, causing even-length repeated segments (like "FooFoo") to not
     * be detected as duplicates.
     */
    @Test
    public void testFormatParameterizedTypeName_oddLengthDuplicateWithSeparator() {
        // Odd-length repeated segment with separator: "Foo.Foo" should be deduplicated to "Foo"
        String input = "Foo.Foo$Inner";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("Foo$Inner", result);
    }

    @Test
    public void testFormatParameterizedTypeName_noDollarSign() {
        // No $ sign - should pass through unchanged
        String input = "java.util.ArrayList";
        String result = ClassUtil.formatParameterizedTypeName(input);
        assertEquals("java.util.ArrayList", result);
    }
}
