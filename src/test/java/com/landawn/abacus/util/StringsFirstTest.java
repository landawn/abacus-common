package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class StringsFirstTest extends StringsTestSupport {
    @Test
    public void testFirstNonEmpty_TwoArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world"));
        assertEquals("world", Strings.firstNonEmpty("", "world"));
        assertEquals("world", Strings.firstNonEmpty(null, "world"));
        assertNull(Strings.firstNonEmpty("", ""));
        assertNull(Strings.firstNonEmpty(null, null));
    }

    @Test
    public void testFirstNonEmpty_ThreeArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world", "!"));
        assertEquals("world", Strings.firstNonEmpty("", "world", "!"));
        assertEquals("!", Strings.firstNonEmpty("", "", "!"));
        assertEquals("!", Strings.firstNonEmpty(null, null, "!"));
        assertNull(Strings.firstNonEmpty("", "", ""));
        assertNull(Strings.firstNonEmpty(null, null, null));
    }

    @Test
    public void testFirstNonEmpty_VarArgs() {
        assertNull(Strings.firstNonEmpty());
        assertNull(Strings.firstNonEmpty((String[]) null));
        assertNull(Strings.firstNonEmpty(null, null, null));
        assertEquals(" ", Strings.firstNonEmpty(null, "", " "));
        assertEquals("abc", Strings.firstNonEmpty("abc"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty("", "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz", "abc"));
    }

    @Test
    public void testFirstNonEmpty_Iterable() {
        assertNull(Strings.firstNonEmpty((Iterable<String>) null));
        assertNull(Strings.firstNonEmpty(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonEmpty(Arrays.asList("", null, "hello")));
        assertNull(Strings.firstNonEmpty(Arrays.asList("", null, "")));
    }

    @Test
    public void testFirstNonEmptyVarArgs() {
        assertEquals("a", Strings.firstNonEmpty(null, "", "a", "b"));
        assertNull(Strings.firstNonEmpty(null, ""));
        assertNull(Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonEmptyIterable() {
        assertEquals("a", Strings.firstNonEmpty(list(null, "", "a", "b")));
        assertNull(Strings.firstNonEmpty(list(null, "")));
        assertNull(Strings.firstNonEmpty(new ArrayList<>()));
        assertNull(Strings.firstNonEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstNonEmpty() {
        assertEquals("test", Strings.firstNonEmpty("", "test", "another"));
        assertEquals("first", Strings.firstNonEmpty("first", "second"));
        assertNull(Strings.firstNonEmpty("", "", null));
        assertNull(Strings.firstNonEmpty());
    }

    @Test
    public void testFirstNonBlank_TwoArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world"));
        assertEquals("world", Strings.firstNonBlank("   ", "world"));
        assertEquals("world", Strings.firstNonBlank(null, "world"));
        assertNull(Strings.firstNonBlank("", ""));
        assertNull(Strings.firstNonBlank(null, null));
        assertNull(Strings.firstNonBlank("  ", "  "));
    }

    @Test
    public void testFirstNonBlank_ThreeArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world", "!"));
        assertEquals("world", Strings.firstNonBlank("   ", "world", "!"));
        assertEquals("!", Strings.firstNonBlank("  ", "", "!"));
        assertEquals("!", Strings.firstNonBlank(null, null, "!"));
        assertNull(Strings.firstNonBlank("", "", ""));
    }

    @Test
    public void testFirstNonBlank_VarArgs() {
        assertNull(Strings.firstNonBlank());
        assertNull(Strings.firstNonBlank((String[]) null));
        assertEquals("abc", Strings.firstNonBlank(null, "  ", "abc"));
        assertNull(Strings.firstNonBlank(null, "", "   "));
    }

    @Test
    public void testFirstNonBlank_Iterable() {
        assertNull(Strings.firstNonBlank((Iterable<String>) null));
        assertNull(Strings.firstNonBlank(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonBlank(Arrays.asList("   ", null, "hello")));
        assertNull(Strings.firstNonBlank(Arrays.asList("  ", null, "")));
    }

    @Test
    public void testFirstNonBlankVarArgs() {
        assertEquals("a", Strings.firstNonBlank(null, " ", "\t", "a", "b"));
        assertNull(Strings.firstNonBlank(null, " ", "\t"));
        assertNull(Strings.firstNonBlank());
    }

    @Test
    public void testFirstNonBlankIterable() {
        assertEquals("a", Strings.firstNonBlank(list(null, " ", "\t", "a", "b")));
        assertNull(Strings.firstNonBlank(list(null, " ", "\t")));
        assertNull(Strings.firstNonBlank(new ArrayList<>()));
        assertNull(Strings.firstNonBlank((Iterable<String>) null));
    }

    @Test
    public void testFirstNonBlank() {
        assertEquals("test", Strings.firstNonBlank("", " ", "test"));
        assertEquals("first", Strings.firstNonBlank("first", "second"));
        assertNull(Strings.firstNonBlank("", " ", null));
        assertNull(Strings.firstNonBlank());
    }

    @Test
    public void testFirstNonEmpty_VarargsAllEmpty() {
        assertNull(Strings.firstNonEmpty(null, null, null, null));
        assertNull(Strings.firstNonEmpty("", null, "", null));
        assertEquals("x", Strings.firstNonEmpty(null, "", "x", "y"));
    }

    @Test
    public void testFirstNonBlank_VarargsAllBlank() {
        assertNull(Strings.firstNonBlank(null, "  ", "  ", null));
        assertNull(Strings.firstNonBlank("", " ", "\t", null));
        assertEquals("x", Strings.firstNonBlank(null, "  ", "x", "y"));
    }

    @Test
    public void testFirstNonEmpty_charSequence() {
        final StringBuilder empty = new StringBuilder();
        final StringBuilder hello = new StringBuilder("hello");
        final StringBuilder world = new StringBuilder("world");

        assertSame(hello, Strings.firstNonEmpty(hello, world));
        assertSame(world, Strings.firstNonEmpty(empty, world));
        assertSame(world, Strings.firstNonEmpty(null, world));
        assertNull(Strings.firstNonEmpty(empty, empty));
        assertNull(Strings.firstNonEmpty((StringBuilder) null, (StringBuilder) null));

        assertSame(hello, Strings.firstNonEmpty(hello, world, empty));
        assertSame(world, Strings.firstNonEmpty(empty, world, hello));
        assertSame(hello, Strings.firstNonEmpty(empty, null, hello));
        assertNull(Strings.firstNonEmpty(empty, null, empty));

        assertSame(world, Strings.firstNonEmpty(new StringBuilder[] { empty, null, world, hello }));
        assertNull(Strings.firstNonEmpty(new StringBuilder[] { empty, null }));
        assertNull(Strings.firstNonEmpty((StringBuilder[]) null));

        assertSame(world, Strings.firstNonEmpty(Arrays.asList(empty, null, world, hello)));
        assertNull(Strings.firstNonEmpty(Arrays.asList(empty, null)));
        assertNull(Strings.firstNonEmpty((Iterable<StringBuilder>) null));
        assertNull(Strings.firstNonEmpty(new ArrayList<StringBuilder>()));
    }

    @Test
    public void testFirstNonBlank_charSequence() {
        final StringBuilder blank = new StringBuilder("  \t");
        final StringBuilder hello = new StringBuilder("hello");
        final StringBuilder world = new StringBuilder("world");

        assertSame(hello, Strings.firstNonBlank(hello, world));
        assertSame(world, Strings.firstNonBlank(blank, world));
        assertSame(world, Strings.firstNonBlank(null, world));
        assertNull(Strings.firstNonBlank(blank, blank));
        assertNull(Strings.firstNonBlank((StringBuilder) null, (StringBuilder) null));

        assertSame(hello, Strings.firstNonBlank(hello, world, blank));
        assertSame(world, Strings.firstNonBlank(blank, world, hello));
        assertSame(hello, Strings.firstNonBlank(blank, null, hello));
        assertNull(Strings.firstNonBlank(blank, null, blank));

        assertSame(world, Strings.firstNonBlank(new StringBuilder[] { blank, null, world, hello }));
        assertNull(Strings.firstNonBlank(new StringBuilder[] { blank, null }));
        assertNull(Strings.firstNonBlank((StringBuilder[]) null));

        assertSame(world, Strings.firstNonBlank(Arrays.asList(blank, null, world, hello)));
        assertNull(Strings.firstNonBlank(Arrays.asList(blank, null)));
        assertNull(Strings.firstNonBlank((Iterable<StringBuilder>) null));
        assertNull(Strings.firstNonBlank(new ArrayList<StringBuilder>()));
    }

    @Test
    public void testFirstNonEmptyBlank_heterogeneousCharSequences() {
        final CharSequence buffer = new StringBuffer("  ");
        final CharSequence builder = new StringBuilder("built");
        final List<CharSequence> mixed = Arrays.asList("", null, buffer, builder, "tail");

        assertSame(buffer, Strings.firstNonEmpty(mixed));
        assertSame(builder, Strings.firstNonBlank(mixed));

        assertSame(buffer, Strings.firstNonEmpty(mixed.toArray(new CharSequence[0])));
        assertSame(builder, Strings.firstNonBlank(mixed.toArray(new CharSequence[0])));

        // Mixing the argument types in a fixed-arity call is fine; the result is their common supertype.
        final CharSequence firstOfTwo = Strings.firstNonEmpty(new StringBuilder(), "second");
        assertEquals("second", firstOfTwo);

        // Iterable<? extends T> - a List of a subtype binds without an explicit witness.
        final List<StringBuilder> builders = Arrays.asList(new StringBuilder(), new StringBuilder("x"));
        final StringBuilder found = Strings.firstNonEmpty(builders);
        assertEquals("x", found.toString());
    }

    @Test
    public void testFirstNonEmptyBlank_stringInferenceUnchanged() {
        // Each of these compiles only while T infers to String rather than to CharSequence.
        final String twoArgs = Strings.firstNonEmpty("", "b");
        final String threeArgs = Strings.firstNonEmpty("", "", "c");
        final String varArgs = Strings.firstNonEmpty("", "", "", "d");
        final String iterable = Strings.firstNonEmpty(Arrays.asList("", "e"));
        assertEquals("b", twoArgs);
        assertEquals("c", threeArgs);
        assertEquals("d", varArgs);
        assertEquals("e", iterable);

        final String blankTwo = Strings.firstNonBlank(" ", "b");
        final String blankThree = Strings.firstNonBlank(" ", " ", "c");
        final String blankVar = Strings.firstNonBlank(" ", " ", " ", "d");
        final String blankIterable = Strings.firstNonBlank(Arrays.asList(" ", "e"));
        assertEquals("b", blankTwo);
        assertEquals("c", blankThree);
        assertEquals("d", blankVar);
        assertEquals("e", blankIterable);

        // String literals still reach the fixed-arity overloads; the varargs form still accepts zero arguments.
        assertNull(Strings.firstNonEmpty());
        assertNull(Strings.firstNonBlank());
        assertSame("only", Strings.firstNonEmpty("only"));
    }

    @Test
    public void testFirstNonEmptyBlank_agreeWithN() {
        final List<List<CharSequence>> inputs = new ArrayList<>();
        inputs.add(new ArrayList<>());
        inputs.add(Arrays.asList((CharSequence) null));
        inputs.add(Arrays.asList("", new StringBuilder()));
        inputs.add(Arrays.asList("", new StringBuilder("  "), "x"));
        inputs.add(Arrays.asList(new StringBuffer("  "), (CharSequence) null, new StringBuilder("y")));

        for (final List<CharSequence> input : inputs) {
            final CharSequence[] array = input.toArray(new CharSequence[0]);

            assertEquals(CommonUtil.firstNonEmpty(input).orElseNull(), Strings.firstNonEmpty(input), input.toString());
            assertEquals(CommonUtil.firstNonBlank(input).orElseNull(), Strings.firstNonBlank(input), input.toString());
            assertEquals(CommonUtil.firstNonEmpty(array).orElseNull(), Strings.firstNonEmpty(array), input.toString());
            assertEquals(CommonUtil.firstNonBlank(array).orElseNull(), Strings.firstNonBlank(array), input.toString());
        }
    }
}
