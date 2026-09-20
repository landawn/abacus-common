package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.base64EncodeString;
import static com.landawn.abacus.util.Strings.isBase64;
import static com.landawn.abacus.util.Strings.isBase64Alphabet;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

public class StringsIsTest extends StringsTestSupport {

    @Test
    public void testIsValidJavaIdentifier() {
        assertTrue(Strings.isValidJavaIdentifier("foo"));
        assertTrue(Strings.isValidJavaIdentifier("orderId"));
        assertTrue(Strings.isValidJavaIdentifier("myVar123"));
        assertTrue(Strings.isValidJavaIdentifier("_var"));
        assertTrue(Strings.isValidJavaIdentifier("$bar"));
        assertTrue(Strings.isValidJavaIdentifier("MAX_VALUE"));
        assertTrue(Strings.isValidJavaIdentifier("when"));
        assertTrue(Strings.isValidJavaIdentifier("var1"));
        assertTrue(Strings.isValidJavaIdentifier("recordValue"));
        assertTrue(Strings.isValidJavaIdentifier("yielder"));

        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
        assertFalse(Strings.isValidJavaIdentifier("  "));
        assertFalse(Strings.isValidJavaIdentifier("123var"));
        assertFalse(Strings.isValidJavaIdentifier("my-Var"));
        assertFalse(Strings.isValidJavaIdentifier("a b"));
        assertFalse(Strings.isValidJavaIdentifier("$gee ks123"));

        assertFalse(Strings.isValidJavaIdentifier("class"));
        assertFalse(Strings.isValidJavaIdentifier("int"));
        assertFalse(Strings.isValidJavaIdentifier("final"));
        assertFalse(Strings.isValidJavaIdentifier("for"));
        assertFalse(Strings.isValidJavaIdentifier("default"));
        assertFalse(Strings.isValidJavaIdentifier("true"));
        assertFalse(Strings.isValidJavaIdentifier("null"));
    }

    @Test
    public void testIsValidJavaIdentifier_SupplementaryUnicode() {
        final String deseretCapital = new String(Character.toChars(0x10400));

        assertTrue(Character.isJavaIdentifierStart(0x10400));
        assertTrue(Strings.isValidJavaIdentifier(deseretCapital + "Value"));
        assertTrue(Strings.isValidJavaIdentifier("value" + deseretCapital));
        assertFalse(Strings.isValidJavaIdentifier("\uD801Value"));
        assertFalse(Strings.isValidJavaIdentifier("value\uDC00"));
    }

    @Test
    public void testIsJavaKeyword() {
        assertTrue(Strings.isJavaKeyword("class"));
        assertTrue(Strings.isJavaKeyword("public"));
        assertTrue(Strings.isJavaKeyword("if"));
        assertTrue(Strings.isJavaKeyword("return"));
        assertTrue(Strings.isJavaKeyword("void"));
        assertTrue(Strings.isJavaKeyword("abstract"));
        assertTrue(Strings.isJavaKeyword("int"));
        assertTrue(Strings.isJavaKeyword("for"));

        assertFalse(Strings.isJavaKeyword("Class"));
        assertFalse(Strings.isJavaKeyword("hello"));
        assertFalse(Strings.isJavaKeyword("myVar"));
        assertFalse(Strings.isJavaKeyword(null));
        assertFalse(Strings.isJavaKeyword(""));
    }

    @Test
    public void testIsJavaKeyword_RestrictedIdentifiers() {
        assertFalse(Strings.isJavaKeyword("var"));
        assertFalse(Strings.isJavaKeyword("yield"));
        assertFalse(Strings.isJavaKeyword("record"));
        assertFalse(Strings.isJavaKeyword("when"));
        assertEquals(javax.lang.model.SourceVersion.isKeyword("when"), Strings.isJavaKeyword("when"));
    }

    @Test
    public void testIsValidEmailAddress() {
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("user.name@example.co.uk"));
        assertTrue(Strings.isValidEmailAddress("user+tag@example.com"));
        assertTrue(Strings.isValidEmailAddress("admin@domain.org"));
        assertTrue(Strings.isValidEmailAddress("\"quoted.user\"@example.com"));
        assertTrue(Strings.isValidEmailAddress("user@[127.0.0.1]"));
        assertTrue(Strings.isValidEmailAddress("user.name+tag+sorting@example.com"));

        assertFalse(Strings.isValidEmailAddress("test@example"));
        assertFalse(Strings.isValidEmailAddress("test"));
        assertFalse(Strings.isValidEmailAddress("user name@example.com"));
        assertFalse(Strings.isValidEmailAddress("@example.com"));
        assertFalse(Strings.isValidEmailAddress("test@"));
        assertFalse(Strings.isValidEmailAddress("test.example.com"));
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
    }

    @Test
    public void testIsValidUrl() {
        assertTrue(Strings.isValidUrl("http://example.com"));
        assertTrue(Strings.isValidUrl("https://example.com/path?query=val#fragment"));
        assertTrue(Strings.isValidUrl("ftp://user:pass@example.com:21/path"));
        assertTrue(Strings.isValidUrl("http://example.com:8080/path"));
        assertTrue(Strings.isValidUrl("http://example.com:65535/path"));
        assertTrue(Strings.isValidUrl("file:///C:/Users/test.txt"));
        assertTrue(Strings.isValidUrl("file://server/share/test.txt"));

        assertFalse(Strings.isValidUrl("not a url"));
        assertFalse(Strings.isValidUrl("www.example.com"));
        assertFalse(Strings.isValidUrl("example.com"));
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
        assertFalse(Strings.isValidUrl("http://a<script>"));
        assertFalse(Strings.isValidUrl("http://exa mple.com"));
        assertFalse(Strings.isValidUrl("http://foo_bar.com"));
        assertFalse(Strings.isValidUrl("http://999.999.999.999"));
        assertFalse(Strings.isValidUrl("http://example.com:65536/"));
        assertFalse(Strings.isValidUrl("ftp://example.com:99999/"));
        assertFalse(Strings.isValidUrl("javascript:alert(1)"));
        assertFalse(Strings.isValidUrl("mailto:user@example.com"));
        assertFalse(Strings.isValidUrl("file://user:pass@example.com/path"));
        assertFalse(Strings.isValidUrl("file://user@example.com/path"));
        assertFalse(Strings.isValidUrl("file://example.com:8080/path"));
        assertFalse(Strings.isValidUrl("file://example.com:/path"));
        assertFalse(Strings.isValidUrl("file://foo_bar/path"));
        assertFalse(Strings.isValidUrl("file://C:/Users/doc.txt"));
    }

    @Test
    public void testIsValidHttpUrl() {
        assertTrue(Strings.isValidHttpUrl("http://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com:8443/path"));
        assertTrue(Strings.isValidHttpUrl("https://example.com:65535/path"));
        assertTrue(Strings.isValidHttpUrl("http://localhost:8080"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/foo-bar"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/search?q=hello+world"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/a%20b"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/~user"));
        assertTrue(Strings.isValidHttpUrl("https://example.com/path?query=val#frag-ment"));
        assertEquals(Strings.isValidUrl("https://example.com/foo-bar"), Strings.isValidHttpUrl("https://example.com/foo-bar"));
        assertEquals(Strings.isValidUrl("http://."), Strings.isValidHttpUrl("http://."));

        assertFalse(Strings.isValidHttpUrl("ftp://example.com"));
        assertFalse(Strings.isValidHttpUrl("file:///C:/doc.txt"));
        assertFalse(Strings.isValidHttpUrl("www.example.com"));
        assertFalse(Strings.isValidHttpUrl("http://."));
        assertFalse(Strings.isValidHttpUrl("http://a<script>"));
        assertFalse(Strings.isValidHttpUrl("https://foo_bar.com"));
        assertFalse(Strings.isValidHttpUrl("http://example.com:65536/"));
        assertFalse(Strings.isValidHttpUrl("https://example.com:99999/"));
        assertFalse(Strings.isValidHttpUrl("htp://example.com"));
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertTrue(Strings.isEmpty(new StringBuilder()));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("abc"));
        assertFalse(Strings.isEmpty("  abc  "));
        assertFalse(Strings.isEmpty(new StringBuilder("test")));
    }

    @Test
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("   \t\n\r  "));
        assertTrue(Strings.isBlank("\t"));
        assertTrue(Strings.isBlank("\n"));
        assertTrue(Strings.isBlank(" \t\n\r\f"));
        assertFalse(Strings.isBlank("abc"));
        assertFalse(Strings.isBlank("  abc  "));
        assertFalse(Strings.isBlank("\u00A0"));
    }

    @Test
    public void testIsBlank_NonStringCharSequence() {
        assertTrue(Strings.isBlank(new StringBuilder("   ")));
        assertTrue(Strings.isBlank(new StringBuilder("\t\n\r")));
        assertTrue(Strings.isBlank(new StringBuilder(" \t\n\r\f")));
        assertFalse(Strings.isBlank(new StringBuilder("abc")));
        assertFalse(Strings.isBlank(new StringBuilder("\u00A0")));

        assertEquals(Strings.isBlank("   "), Strings.isBlank(new StringBuilder("   ")));
        assertEquals(Strings.isBlank("\t\n\r"), Strings.isBlank(new StringBuilder("\t\n\r")));
        assertEquals(Strings.isBlank("abc"), Strings.isBlank(new StringBuilder("abc")));
        assertEquals(Strings.isBlank("\u00A0"), Strings.isBlank(new StringBuilder("\u00A0")));

        final String grinningFace = "\uD83D\uDE00";
        assertFalse(Strings.isBlank(grinningFace));
        assertFalse(Strings.isBlank(new StringBuilder(grinningFace)));
        assertEquals(Strings.isBlank(grinningFace), Strings.isBlank(new StringBuilder(grinningFace)));
    }

    @Test
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("abc"));
        assertTrue(Strings.isNotEmpty("  abc  "));
    }

    @Test
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertFalse(Strings.isNotBlank("   \t\n\r  "));
        assertTrue(Strings.isNotBlank("abc"));
        assertTrue(Strings.isNotBlank("  abc  "));
    }

    @Test
    public void testIsAllEmpty() {
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertTrue(Strings.isAllEmpty("", null));
        assertFalse(Strings.isAllEmpty("abc", ""));
        assertFalse(Strings.isAllEmpty("", "xyz"));
        assertFalse(Strings.isAllEmpty("abc", "xyz"));
        assertFalse(Strings.isAllEmpty(" ", ""));
        assertFalse(Strings.isAllEmpty(null, "foo"));

        assertTrue(Strings.isAllEmpty(null, null, null));
        assertTrue(Strings.isAllEmpty("", "", ""));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertFalse(Strings.isAllEmpty("abc", "", ""));
        assertFalse(Strings.isAllEmpty("", "xyz", ""));
        assertFalse(Strings.isAllEmpty("", "", "123"));
        assertFalse(Strings.isAllEmpty("", null, "test"));
    }

    @Test
    public void testIsAllEmpty_Varargs() {
        assertTrue(Strings.isAllEmpty());
        assertTrue(Strings.isAllEmpty((CharSequence[]) null));
        assertTrue(Strings.isAllEmpty((CharSequence) null));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertTrue(Strings.isAllEmpty(null, "", null, ""));
        assertFalse(Strings.isAllEmpty(null, "foo", ""));
        assertFalse(Strings.isAllEmpty("", "bar", null));
        assertFalse(Strings.isAllEmpty(" ", "", null));
        assertFalse(Strings.isAllEmpty("a", "b", "c", "d"));
        assertFalse(Strings.isAllEmpty(null, "", "x", null));
    }

    @Test
    public void testIsAllEmpty_Iterable() {
        assertTrue(Strings.isAllEmpty((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllEmpty(new ArrayList<>()));
        assertTrue(Strings.isAllEmpty(Arrays.asList(null, "", null)));
        assertTrue(Strings.isAllEmpty(list(null, "", null)));
        assertFalse(Strings.isAllEmpty(Arrays.asList("", "abc", "")));
        assertFalse(Strings.isAllEmpty(Arrays.asList("abc")));
        assertFalse(Strings.isAllEmpty(list(null, "a", "")));
    }

    @Test
    public void testIsAllBlank() {
        assertTrue(Strings.isAllBlank(null, null));
        assertTrue(Strings.isAllBlank("", ""));
        assertTrue(Strings.isAllBlank("   ", "\t\n"));
        assertTrue(Strings.isAllBlank(null, "   "));
        assertFalse(Strings.isAllBlank("abc", "   "));
        assertFalse(Strings.isAllBlank("   ", "xyz"));
        assertFalse(Strings.isAllBlank("", "test"));

        assertTrue(Strings.isAllBlank(null, null, null));
        assertTrue(Strings.isAllBlank("", "", ""));
        assertTrue(Strings.isAllBlank("   ", "\t", "\n"));
        assertTrue(Strings.isAllBlank("", " ", "\t"));
        assertFalse(Strings.isAllBlank("abc", "   ", ""));
        assertFalse(Strings.isAllBlank("", "xyz", "   "));
        assertFalse(Strings.isAllBlank("", " ", "test"));
    }

    @Test
    public void testIsAllBlank_Varargs() {
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank((CharSequence[]) null));
        assertTrue(Strings.isAllBlank((CharSequence) null));
        assertTrue(Strings.isAllBlank(null, "", "  "));
        assertTrue(Strings.isAllBlank(null, null, " ", "\t"));
        assertTrue(Strings.isAllBlank("  ", null, "", "  "));
        assertFalse(Strings.isAllBlank(null, "foo", "  "));
        assertFalse(Strings.isAllBlank("  ", "bar", null));
        assertFalse(Strings.isAllBlank("a", "b", "c", "d"));
        assertFalse(Strings.isAllBlank("  ", null, "x", "  "));
    }

    @Test
    public void testIsAllBlank_Iterable() {
        assertTrue(Strings.isAllBlank((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllBlank(new ArrayList<>()));
        assertTrue(Strings.isAllBlank(Arrays.asList(null, "", "   ")));
        assertTrue(Strings.isAllBlank(list(null, " ", "\n")));
        assertFalse(Strings.isAllBlank(Arrays.asList("   ", "abc", "")));
        assertFalse(Strings.isAllBlank(list(null, "a", " ")));
    }

    @Test
    public void testIsAnyEmpty() {
        assertTrue(Strings.isAnyEmpty(null, null));
        assertTrue(Strings.isAnyEmpty("", ""));
        assertTrue(Strings.isAnyEmpty("abc", ""));
        assertTrue(Strings.isAnyEmpty("", "xyz"));
        assertTrue(Strings.isAnyEmpty(null, "xyz"));
        assertTrue(Strings.isAnyEmpty("", "test"));
        assertFalse(Strings.isAnyEmpty("abc", "xyz"));
        assertFalse(Strings.isAnyEmpty("   ", "xyz"));
        assertFalse(Strings.isAnyEmpty("hello", "world"));

        assertTrue(Strings.isAnyEmpty(null, null, null));
        assertTrue(Strings.isAnyEmpty("abc", "", "xyz"));
        assertTrue(Strings.isAnyEmpty("", "def", "xyz"));
        assertTrue(Strings.isAnyEmpty("abc", "def", null));
        assertTrue(Strings.isAnyEmpty("hello", "", "world"));
        assertFalse(Strings.isAnyEmpty("abc", "def", "xyz"));
        assertFalse(Strings.isAnyEmpty("   ", "def", "xyz"));
    }

    @Test
    public void testIsAnyEmpty_Varargs() {
        assertFalse(Strings.isAnyEmpty());
        assertFalse(Strings.isAnyEmpty((CharSequence[]) null));
        assertTrue(Strings.isAnyEmpty(null, "foo"));
        assertTrue(Strings.isAnyEmpty("", "bar"));
        assertTrue(Strings.isAnyEmpty("bob", ""));
        assertTrue(Strings.isAnyEmpty("  bob  ", null));
        assertTrue(Strings.isAnyEmpty("a", "b", null, "d"));
        assertFalse(Strings.isAnyEmpty(" ", "bar"));
        assertFalse(Strings.isAnyEmpty("foo", "bar"));
    }

    @Test
    public void testIsAnyEmpty_Iterable() {
        assertFalse(Strings.isAnyEmpty((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyEmpty(new ArrayList<>()));
        assertTrue(Strings.isAnyEmpty(Arrays.asList("abc", "", "xyz")));
        assertTrue(Strings.isAnyEmpty(list("a", "", "b")));
        assertFalse(Strings.isAnyEmpty(Arrays.asList("abc", "def", "xyz")));
        assertFalse(Strings.isAnyEmpty(list("a", "b")));
    }

    @Test
    public void testIsAnyBlank() {
        assertTrue(Strings.isAnyBlank(null, null));
        assertTrue(Strings.isAnyBlank("", ""));
        assertTrue(Strings.isAnyBlank("   ", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "\t\n"));
        assertTrue(Strings.isAnyBlank(null, "xyz"));
        assertTrue(Strings.isAnyBlank(" ", "test"));
        assertFalse(Strings.isAnyBlank("abc", "xyz"));
        assertFalse(Strings.isAnyBlank("hello", "world"));

        assertTrue(Strings.isAnyBlank(null, null, null));
        assertTrue(Strings.isAnyBlank("abc", "   ", "xyz"));
        assertTrue(Strings.isAnyBlank("", "def", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "def", null));
        assertTrue(Strings.isAnyBlank("hello", " ", "world"));
        assertFalse(Strings.isAnyBlank("abc", "def", "xyz"));
    }

    @Test
    public void testIsAnyBlank_Varargs() {
        assertFalse(Strings.isAnyBlank());
        assertFalse(Strings.isAnyBlank((CharSequence[]) null));
        assertTrue(Strings.isAnyBlank(null, "foo"));
        assertTrue(Strings.isAnyBlank("", "bar"));
        assertTrue(Strings.isAnyBlank("  bob  ", null));
        assertTrue(Strings.isAnyBlank(" ", "bar"));
        assertTrue(Strings.isAnyBlank("a", "b", "\t", "d"));
        assertTrue(Strings.isAnyBlank("a", "b", null, "d"));
        assertFalse(Strings.isAnyBlank("foo", "bar"));
        assertFalse(Strings.isAnyBlank("a", "b", "c", "d"));
    }

    @Test
    public void testIsAnyBlank_Iterable() {
        assertFalse(Strings.isAnyBlank((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyBlank(new ArrayList<>()));
        assertTrue(Strings.isAnyBlank(Arrays.asList("abc", "   ", "xyz")));
        assertTrue(Strings.isAnyBlank(list("a", " ", "b")));
        assertFalse(Strings.isAnyBlank(Arrays.asList("abc", "def", "xyz")));
        assertFalse(Strings.isAnyBlank(list("a", "b")));
    }

    @Test
    public void testIsWrappedWith() {
        assertTrue(Strings.isWrappedWith("'hello'", "'"));
        assertTrue(Strings.isWrappedWith("\"text\"", "\""));
        assertTrue(Strings.isWrappedWith("--comment--", "--"));
        assertTrue(Strings.isWrappedWith("wrap_me_wrap", "wrap"));
        assertFalse(Strings.isWrappedWith("hello", "'"));
        assertFalse(Strings.isWrappedWith("'hello\"", "'"));
        assertFalse(Strings.isWrappedWith("[abc]", "["));
        assertFalse(Strings.isWrappedWith("wrap_me_no", "wrap"));
        assertFalse(Strings.isWrappedWith("nowrap_me_wrap", "wrap"));
        assertFalse(Strings.isWrappedWith("short", "longer_wrap"));
        assertFalse(Strings.isWrappedWith(null, "'"));
        assertFalse(Strings.isWrappedWith("''", "''"));
        assertFalse(Strings.isWrappedWith("aaa", "aa"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", ""));
    }

    @Test
    public void testIsWrappedWith_PrefixSuffix() {
        assertTrue(Strings.isWrappedWith("<html>content</html>", "<html>", "</html>"));
        assertTrue(Strings.isWrappedWith("{data}", "{", "}"));
        assertTrue(Strings.isWrappedWith("[array]", "[", "]"));
        assertTrue(Strings.isWrappedWith("(test)", "(", ")"));
        assertFalse(Strings.isWrappedWith("hello", "<", ">"));
        assertFalse(Strings.isWrappedWith("<hello", "<", ">"));
        assertFalse(Strings.isWrappedWith("test", "(", ")"));
        assertFalse(Strings.isWrappedWith("(test", "(", ")"));
        assertFalse(Strings.isWrappedWith("(abc]", "[", "]"));
        assertFalse(Strings.isWrappedWith("[abc)", "[", "]"));
        assertFalse(Strings.isWrappedWith("short", "long_prefix", "]"));
        assertFalse(Strings.isWrappedWith(null, "<", ">"));
        assertFalse(Strings.isWrappedWith("aba", "ab", "ba"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "", ">"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "<", ""));
    }

    @Test
    public void testIsLowerCase() {
        assertTrue(Strings.isLowerCase('a'));
        assertTrue(Strings.isLowerCase('z'));
        assertFalse(Strings.isLowerCase('A'));
        assertFalse(Strings.isLowerCase('1'));
    }

    @Test
    public void testIsAsciiLowerCase() {
        assertTrue(Strings.isAsciiLowerCase('a'));
        assertTrue(Strings.isAsciiLowerCase('z'));
        assertFalse(Strings.isAsciiLowerCase('A'));
        assertFalse(Strings.isAsciiLowerCase('1'));
    }

    @Test
    public void testIsUpperCase() {
        assertTrue(Strings.isUpperCase('A'));
        assertTrue(Strings.isUpperCase('Z'));
        assertFalse(Strings.isUpperCase('a'));
        assertFalse(Strings.isUpperCase('1'));
    }

    @Test
    public void testIsAsciiUpperCase() {
        assertTrue(Strings.isAsciiUpperCase('A'));
        assertTrue(Strings.isAsciiUpperCase('Z'));
        assertFalse(Strings.isAsciiUpperCase('a'));
        assertFalse(Strings.isAsciiUpperCase('1'));
    }

    @Test
    public void testIsAllLowerCase() {
        assertTrue(Strings.isAllLowerCase("abc"));
        assertTrue(Strings.isAllLowerCase("a"));
        assertFalse(Strings.isAllLowerCase("Abc"));
        assertFalse(Strings.isAllLowerCase("ABC"));
        assertFalse(Strings.isAllLowerCase("abc123"));
        assertFalse(Strings.isAllLowerCase(null));
    }

    @Test
    public void testIsAllLowerCase_empty_isVacuouslyTrue() {
        // An empty CharSequence has no code point that is not lowercase, so the universal
        // quantifier is vacuously satisfied. null is a missing input, not an empty one.
        assertTrue(Strings.isAllLowerCase(""));
        assertTrue(Strings.isAllLowerCase(new StringBuilder()));
        assertTrue(Strings.isAllLowerCase(new StringBuilder("ab").subSequence(1, 1)));
        assertFalse(Strings.isAllLowerCase(null));

        // ... and it is simultaneously vacuously all-uppercase, which is only consistent for empty input.
        assertTrue(Strings.isAllUpperCase(""));
        assertFalse(Strings.isMixedCase(""));

        // The sibling character-class predicates deliberately keep returning false for empty input.
        assertFalse(Strings.isAlpha(""));
        assertFalse(Strings.isNumeric(""));
        assertFalse(Strings.isAsciiPrintable(""));
        assertFalse(Strings.isWhitespace(""));
    }

    @Test
    public void testIsAllUpperCase() {
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertTrue(Strings.isAllUpperCase("A"));
        assertFalse(Strings.isAllUpperCase("Abc"));
        assertFalse(Strings.isAllUpperCase("abc"));
        assertFalse(Strings.isAllUpperCase("ABC123"));
        assertFalse(Strings.isAllUpperCase(null));
    }

    @Test
    public void testIsAllUpperCase_empty_isVacuouslyTrue() {
        assertTrue(Strings.isAllUpperCase(""));
        assertTrue(Strings.isAllUpperCase(new StringBuilder()));
        assertTrue(Strings.isAllUpperCase(new StringBuilder("AB").subSequence(2, 2)));
        assertFalse(Strings.isAllUpperCase(null));
    }

    @Test
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("Abc"));
        assertTrue(Strings.isMixedCase("aBc"));
        assertTrue(Strings.isMixedCase("aB"));
        assertFalse(Strings.isMixedCase("abc"));
        assertFalse(Strings.isMixedCase("ABC"));
        assertFalse(Strings.isMixedCase("123"));
        assertFalse(Strings.isMixedCase("a"));
        assertFalse(Strings.isMixedCase("B"));
        assertFalse(Strings.isMixedCase("a黎"));
        assertFalse(Strings.isMixedCase("黎B"));
        assertFalse(Strings.isMixedCase(null));
        assertFalse(Strings.isMixedCase(""));
    }

    @Test
    public void testIsMixedCase_TitleCase() {
        final String titleCaseDz = "ǅ";
        assertFalse(Character.isUpperCase(titleCaseDz.codePointAt(0)));
        assertTrue(Character.isTitleCase(titleCaseDz.codePointAt(0)));

        assertTrue(Strings.isMixedCase(titleCaseDz + "a"));
        assertTrue(Strings.isMixedCase("a" + titleCaseDz));
        assertTrue(Strings.isMixedCase("x" + titleCaseDz + "1"));
        assertFalse(Strings.isMixedCase(titleCaseDz));
        assertFalse(Strings.isMixedCase(titleCaseDz + titleCaseDz));
        assertFalse(Strings.isMixedCase(titleCaseDz + "A"));
        assertFalse(Strings.isAllUpperCase(titleCaseDz));
        assertFalse(Strings.isAllLowerCase(titleCaseDz));

        final String supplementaryLower = new String(Character.toChars(0x10597));
        assertTrue(Strings.isMixedCase(titleCaseDz + supplementaryLower));
    }

    @Test
    public void testIsDigit() {
        assertTrue(Strings.isDigit('0'));
        assertTrue(Strings.isDigit('9'));
        assertFalse(Strings.isDigit('a'));
        assertFalse(Strings.isDigit('A'));
        assertFalse(Strings.isDigit(' '));
    }

    @Test
    public void testIsLetter() {
        assertTrue(Strings.isLetter('a'));
        assertTrue(Strings.isLetter('A'));
        assertTrue(Strings.isLetter('z'));
        assertTrue(Strings.isLetter('Z'));
        assertFalse(Strings.isLetter('1'));
        assertFalse(Strings.isLetter('!'));
        assertFalse(Strings.isLetter(' '));
    }

    @Test
    public void testIsLetterOrDigit() {
        assertTrue(Strings.isLetterOrDigit('a'));
        assertTrue(Strings.isLetterOrDigit('A'));
        assertTrue(Strings.isLetterOrDigit('1'));
        assertFalse(Strings.isLetterOrDigit('!'));
        assertFalse(Strings.isLetterOrDigit(' '));
        assertFalse(Strings.isLetterOrDigit('@'));
    }

    @Test
    public void testIsAscii() {
        assertTrue(Strings.isAscii('a'));
        assertTrue(Strings.isAscii('A'));
        assertTrue(Strings.isAscii('1'));
        assertTrue(Strings.isAscii(' '));
        assertTrue(Strings.isAscii('0'));
        assertFalse(Strings.isAscii('\u00e9'));
        assertFalse(Strings.isAscii('\u00E9'));
    }

    @Test
    public void testIsAsciiPrintable() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('A'));
        assertTrue(Strings.isAsciiPrintable('1'));
        assertTrue(Strings.isAsciiPrintable(' '));
        assertFalse(Strings.isAsciiPrintable('\t'));
        assertFalse(Strings.isAsciiPrintable('\n'));

        assertTrue(Strings.isAsciiPrintable("abc123"));
        assertTrue(Strings.isAsciiPrintable("abc 123"));
        assertTrue(Strings.isAsciiPrintable("hello"));
        assertFalse(Strings.isAsciiPrintable("abc\t123"));
        assertFalse(Strings.isAsciiPrintable("hello\n"));
        assertFalse(Strings.isAsciiPrintable("éclair"));
        assertFalse(Strings.isAsciiPrintable(null));
        assertFalse(Strings.isAsciiPrintable(""));
    }

    @Test
    public void testIsAsciiControl() {
        assertTrue(Strings.isAsciiControl('\t'));
        assertTrue(Strings.isAsciiControl('\n'));
        assertTrue(Strings.isAsciiControl('\0'));
        assertFalse(Strings.isAsciiControl('a'));
        assertFalse(Strings.isAsciiControl('A'));
        assertFalse(Strings.isAsciiControl(' '));
    }

    @Test
    public void testIsAsciiAlpha() {
        assertTrue(Strings.isAsciiAlpha('a'));
        assertTrue(Strings.isAsciiAlpha('Z'));
        assertFalse(Strings.isAsciiAlpha('1'));
        assertFalse(Strings.isAsciiAlpha(' '));
        assertFalse(Strings.isAsciiAlpha('@'));

        assertTrue(Strings.isAsciiAlpha("hello"));
        assertTrue(Strings.isAsciiAlpha("abc"));
        assertTrue(Strings.isAsciiAlpha("ABC"));
        assertFalse(Strings.isAsciiAlpha("hello1"));
        assertFalse(Strings.isAsciiAlpha("abc123"));
        assertFalse(Strings.isAsciiAlpha(null));
        assertFalse(Strings.isAsciiAlpha(""));
    }

    @Test
    public void testIsAsciiNumeric() {
        assertTrue(Strings.isAsciiNumeric('5'));
        assertTrue(Strings.isAsciiNumeric('0'));
        assertTrue(Strings.isAsciiNumeric('9'));
        assertFalse(Strings.isAsciiNumeric('a'));
        assertFalse(Strings.isAsciiNumeric(' '));

        assertTrue(Strings.isAsciiNumeric("12345"));
        assertTrue(Strings.isAsciiNumeric("0"));
        assertFalse(Strings.isAsciiNumeric("123a"));
        assertFalse(Strings.isAsciiNumeric("abc"));
        assertFalse(Strings.isAsciiNumeric("12.3"));
        assertFalse(Strings.isAsciiNumeric(null));
        assertFalse(Strings.isAsciiNumeric(""));
    }

    @Test
    public void testIsAsciiAlphanumeric() {
        assertTrue(Strings.isAsciiAlphanumeric('a'));
        assertTrue(Strings.isAsciiAlphanumeric('A'));
        assertTrue(Strings.isAsciiAlphanumeric('1'));
        assertTrue(Strings.isAsciiAlphanumeric('Z'));
        assertTrue(Strings.isAsciiAlphanumeric('5'));
        assertFalse(Strings.isAsciiAlphanumeric(' '));
        assertFalse(Strings.isAsciiAlphanumeric('!'));
        assertFalse(Strings.isAsciiAlphanumeric('@'));

        assertTrue(Strings.isAsciiAlphanumeric("hello123"));
        assertTrue(Strings.isAsciiAlphanumeric("abc123"));
        assertTrue(Strings.isAsciiAlphanumeric("ABC"));
        assertFalse(Strings.isAsciiAlphanumeric("hello 123"));
        assertFalse(Strings.isAsciiAlphanumeric("abc 123"));
        assertFalse(Strings.isAsciiAlphanumeric(null));
        assertFalse(Strings.isAsciiAlphanumeric(""));
    }

    @Test
    public void testIsAsciiAlphaSpace() {
        assertTrue(Strings.isAsciiAlphaSpace("abc"));
        assertTrue(Strings.isAsciiAlphaSpace("abc def"));
        assertFalse(Strings.isAsciiAlphaSpace("abc123"));
        assertFalse(Strings.isAsciiAlphaSpace(null));
        assertFalse(Strings.isAsciiAlphaSpace(""));
    }

    @Test
    public void testIsAsciiAlphanumericSpace() {
        assertTrue(Strings.isAsciiAlphanumericSpace("abc123"));
        assertTrue(Strings.isAsciiAlphanumericSpace("abc 123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc@123"));
        assertFalse(Strings.isAsciiAlphanumericSpace(null));
        assertFalse(Strings.isAsciiAlphanumericSpace(""));
    }

    @Test
    public void testIsAlpha() {
        assertTrue(Strings.isAlpha("abc"));
        assertTrue(Strings.isAlpha("ABC"));
        assertFalse(Strings.isAlpha("abc123"));
        assertFalse(Strings.isAlpha("abc "));
        assertFalse(Strings.isAlpha("abc def"));
        assertFalse(Strings.isAlpha(null));
        assertFalse(Strings.isAlpha(""));
    }

    @Test
    public void testIsAlphaSpace() {
        assertTrue(Strings.isAlphaSpace("abc"));
        assertTrue(Strings.isAlphaSpace("abc def"));
        assertFalse(Strings.isAlphaSpace("abc123"));
        assertFalse(Strings.isAlphaSpace(null));
        assertFalse(Strings.isAlphaSpace(""));
    }

    @Test
    public void testIsAlphanumeric() {
        assertTrue(Strings.isAlphanumeric("abc123"));
        assertTrue(Strings.isAlphanumeric("ABC"));
        assertTrue(Strings.isAlphanumeric("abc"));
        assertTrue(Strings.isAlphanumeric("123"));
        assertFalse(Strings.isAlphanumeric("abc 123"));
        assertFalse(Strings.isAlphanumeric(null));
        assertFalse(Strings.isAlphanumeric(""));
    }

    @Test
    public void testIsAlphanumericSpace() {
        assertTrue(Strings.isAlphanumericSpace("abc123"));
        assertTrue(Strings.isAlphanumericSpace("abc 123"));
        assertTrue(Strings.isAlphanumericSpace("你好 123"));
        assertTrue(Strings.isAlphanumericSpace("café au lait 2023"));
        assertFalse(Strings.isAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAlphanumericSpace("abc@123"));
        assertFalse(Strings.isAlphanumericSpace(null));
        assertFalse(Strings.isAlphanumericSpace(""));
    }

    @Test
    public void testIsNumeric() {
        assertTrue(Strings.isNumeric("123"));
        assertTrue(Strings.isNumeric("0"));
        assertFalse(Strings.isNumeric("12.3"));
        assertFalse(Strings.isNumeric("-123"));
        assertFalse(Strings.isNumeric("abc"));
        assertFalse(Strings.isNumeric(null));
        assertFalse(Strings.isNumeric(""));
    }

    @Test
    public void testIsNumericSpace() {
        assertTrue(Strings.isNumericSpace("123"));
        assertTrue(Strings.isNumericSpace("1 2 3"));
        assertTrue(Strings.isNumericSpace("123 456"));
        assertFalse(Strings.isNumericSpace("12.3"));
        assertFalse(Strings.isNumericSpace(null));
        assertFalse(Strings.isNumericSpace(""));
    }

    @Test
    public void testIsWhitespace() {
        assertTrue(Strings.isWhitespace("   "));
        assertTrue(Strings.isWhitespace("\t\n\r"));
        assertTrue(Strings.isWhitespace(" "));
        assertFalse(Strings.isWhitespace("abc"));
        assertFalse(Strings.isWhitespace(" a "));
        assertFalse(Strings.isWhitespace(" abc "));
        assertFalse(Strings.isWhitespace(null));
        assertFalse(Strings.isWhitespace(""));
    }

    @Test
    public void testIsAsciiInteger() {
        assertTrue(Strings.isAsciiInteger("123"));
        assertTrue(Strings.isAsciiInteger("-123"));
        assertTrue(Strings.isAsciiInteger("+123"));
        assertFalse(Strings.isAsciiInteger("12.3"));
        assertFalse(Strings.isAsciiInteger("abc"));
        assertFalse(Strings.isAsciiInteger(null));
        assertFalse(Strings.isAsciiInteger(""));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testIsBase64Alphabet() {
        assertTrue(Strings.isBase64Alphabet((byte) 'A'));
        assertTrue(Strings.isBase64Alphabet((byte) 'Z'));
        assertTrue(Strings.isBase64Alphabet((byte) 'a'));
        assertTrue(Strings.isBase64Alphabet((byte) 'z'));
        assertTrue(Strings.isBase64Alphabet((byte) '0'));
        assertTrue(Strings.isBase64Alphabet((byte) '9'));
        assertTrue(Strings.isBase64Alphabet((byte) '+'));
        assertTrue(Strings.isBase64Alphabet((byte) '/'));
        assertTrue(Strings.isBase64Alphabet((byte) '='));
        assertFalse(Strings.isBase64Alphabet((byte) '@'));
        assertFalse(Strings.isBase64Alphabet((byte) '['));
        assertFalse(Strings.isBase64Alphabet((byte) '`'));
        assertFalse(Strings.isBase64Alphabet((byte) '{'));
        assertFalse(Strings.isBase64Alphabet((byte) '!'));
        assertFalse(isBase64Alphabet((byte) '#'));
        assertFalse(isBase64Alphabet((byte) ' '));
    }

    @Test
    public void testIsBase64() {
        assertTrue(Strings.isBase64("SGVsbG8="));
        assertTrue(Strings.isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(Strings.isBase64("V29ybGQ="));
        assertTrue(Strings.isBase64("YWJjZGVm"));
        assertTrue(Strings.isBase64(""));
        assertTrue(isBase64(base64EncodeString("test")));
        assertTrue(Strings.isBase64("SGVsbG8=".getBytes()));
        assertTrue(Strings.isBase64("V29ybGQ=".getBytes()));
        assertTrue(Strings.isBase64(new byte[0]));
        assertTrue(isBase64("".getBytes()));

        assertFalse(Strings.isBase64("Hello@World"));
        assertFalse(Strings.isBase64("Hello World!"));
        assertFalse(Strings.isBase64("ABC!@#"));
        assertFalse(Strings.isBase64("SGVs bG8="));
        assertFalse(Strings.isBase64("not base64!!!"));
        assertFalse(Strings.isBase64((String) null));
        assertFalse(Strings.isBase64((byte[]) null));
        assertFalse(Strings.isBase64("Hello@World".getBytes()));
        assertFalse(Strings.isBase64("SGVs bG8=".getBytes()));
        assertTrue(Strings.isBase64Mime("SGVs bG8=".getBytes()));
        assertTrue(Strings.isBase64Mime("SGVs bG8="));
    }

    @Test
    public void testIsBase64_CompleteStructure() {
        assertTrue(isBase64(""));
        assertTrue(isBase64("TQ=="));
        assertTrue(isBase64("TWE="));
        assertTrue(isBase64("TWFu"));
        assertTrue(isBase64("TQ"));
        assertTrue(isBase64("TWE"));
        assertTrue(isBase64("+/8="));

        assertFalse(isBase64("===="));
        assertFalse(isBase64("A"));
        assertFalse(isBase64("AA=A"));
        assertFalse(isBase64("TQ==="));
        assertFalse(isBase64("TQ==AA"));
        assertFalse(isBase64("SGVs bG8="));
        assertFalse(isBase64("-_8="));
        assertFalse(isBase64("AA+_"));

        assertTrue(isBase64("TQ==".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(isBase64("====".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(isBase64("TQ==\r\n".getBytes(StandardCharsets.US_ASCII)));
    }

    @Test
    public void testIsBase64Url() {
        assertTrue(Strings.isBase64Url(""));
        assertTrue(Strings.isBase64Url("-_8="));
        assertTrue(Strings.isBase64Url("-_8"));
        assertFalse(Strings.isBase64Url("+/8="));
        assertFalse(Strings.isBase64Url("AA-_+"));
        assertFalse(Strings.isBase64Url("===="));
        assertFalse(Strings.isBase64Url("A"));
        assertFalse(Strings.isBase64Url("AA=A"));
        assertFalse(Strings.isBase64Url("TQ==\n"));
        assertFalse(Strings.isBase64Url((String) null));

        assertTrue(Strings.isBase64Url("-_8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Url("+/8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Url((byte[]) null));
    }

    @Test
    public void testIsBase64Mime() {
        assertTrue(Strings.isBase64Mime(""));
        assertTrue(Strings.isBase64Mime(" \t\r\n"));
        assertTrue(Strings.isBase64Mime("SGVs bG8="));
        assertTrue(Strings.isBase64Mime("SGVs\r\nbG8="));
        assertThrows(IllegalArgumentException.class, () -> Strings.base64Decode("SGVs bG8="));
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Base64.getMimeDecoder().decode("SGVs bG8="));
        assertFalse(Strings.isBase64Mime("===="));
        assertFalse(Strings.isBase64Mime("A"));
        assertFalse(Strings.isBase64Mime("SGV!sbG8="));
        assertArrayEquals("Hello".getBytes(StandardCharsets.US_ASCII), Base64.getMimeDecoder().decode("SGV!sbG8="));
        assertFalse(Strings.isBase64Mime("SGVs-bG8="));
        assertFalse(Strings.isBase64Mime("TQ=\r\nQ"));
        assertFalse(Strings.isBase64Mime((String) null));

        assertTrue(Strings.isBase64Mime("SGVs\r\nbG8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Mime("SGV!sbG8=".getBytes(StandardCharsets.US_ASCII)));
        assertFalse(Strings.isBase64Mime((byte[]) null));
    }

    /**
     * Contract pin for the Commons Lang comparison note on {@code isAsciiPrintable(CharSequence)}: only the EMPTY
     * result diverges - both libraries return {@code false} for {@code null}.
     */
    @Test
    public void testIsAsciiPrintable_OnlyEmptyDivergesFromCommonsLang() {
        assertFalse(Strings.isAsciiPrintable((CharSequence) null));
        assertFalse(Strings.isAsciiPrintable(""));
        assertTrue(Strings.isAsciiPrintable("ab"));

        assertFalse(org.apache.commons.lang3.StringUtils.isAsciiPrintable(null));
        assertTrue(org.apache.commons.lang3.StringUtils.isAsciiPrintable(""));
        assertTrue(org.apache.commons.lang3.StringUtils.isAsciiPrintable("ab"));
    }
}
