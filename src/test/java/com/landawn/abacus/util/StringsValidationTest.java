package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.substring;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

/** Strings validation contracts, including regression cases. */
public class StringsValidationTest extends StringsTestSupport {



    // B1 : isValidJavaIdentifier and the JLS 17 restricted identifiers
    private static final String[] RESTRICTED = { "permits", "record", "sealed", "var", "yield" };

    @Test
    public void test_findEmail() {
        final String str = "*** test@gmail.orgg&&^ test2@gmail.cn ((& ";
        assertTrue(Strings.isValidEmailAddress("test@gmail.com"));
        assertEquals("test@gmail.orgg", Strings.findFirstEmailAddress(str));
        assertEquals(List.of("test@gmail.orgg", "test2@gmail.cn"), Strings.findAllEmailAddresses(str));
    }

    @Test
    public void testGuid() {
        String guid = Strings.uuidWithoutHyphens();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
        assertTrue(guid.matches("[a-f0-9]{32}"));
    }

    @Test
    public void testGuid_Unique() {
        String guid1 = Strings.uuidWithoutHyphens();
        String guid2 = Strings.uuidWithoutHyphens();
        assertFalse(guid1.equals(guid2));
    }

    // isAllEmpty

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
    public void testIsAlphaSpace() {
        assertTrue(Strings.isAlphaSpace("abc"));
        assertTrue(Strings.isAlphaSpace("abc def"));
        assertFalse(Strings.isAlphaSpace("abc123"));
        assertFalse(Strings.isAlphaSpace(null));
        assertFalse(Strings.isAlphaSpace(""));
    }

    // isAnyEmpty

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
    public void testIsAnyEmpty_Iterable() {
        assertFalse(Strings.isAnyEmpty((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyEmpty(new ArrayList<>()));
        assertTrue(Strings.isAnyEmpty(Arrays.asList("abc", "", "xyz")));
        assertTrue(Strings.isAnyEmpty(list("a", "", "b")));
        assertFalse(Strings.isAnyEmpty(Arrays.asList("abc", "def", "xyz")));
        assertFalse(Strings.isAnyEmpty(list("a", "b")));
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



    // C-007: the rewritten varargs examples really exercise the varargs overloads
    @Test
    public void testC007_varargsIsAllIsAnyExamples() {
        assertTrue(Strings.isAllEmpty(null, "", null, ""));
        assertFalse(Strings.isAllEmpty("", "", "", "foo"));
        assertFalse(Strings.isAllEmpty(" ", "", null, ""));
        assertTrue(Strings.isAllEmpty((String[]) null));
        assertTrue(Strings.isAllEmpty(new String[] {}));
        assertTrue(Strings.isAllEmpty(new String[] { "", null }));

        assertTrue(Strings.isAllBlank(null, "", " ", "  "));
        assertFalse(Strings.isAllBlank(" ", "", null, "bar"));
        assertTrue(Strings.isAllBlank((String[]) null));
        assertTrue(Strings.isAllBlank(new String[] {}));
        assertTrue(Strings.isAllBlank(new String[] { " ", null }));

        assertTrue(Strings.isAnyEmpty("a", "b", "c", ""));
        assertTrue(Strings.isAnyEmpty("a", null, "c", "d"));
        assertFalse(Strings.isAnyEmpty("a", " ", "c", "d"));
        assertFalse(Strings.isAnyEmpty("a", "b", "c", "d"));
        assertTrue(Strings.isAnyEmpty((String) null));
        assertFalse(Strings.isAnyEmpty((String[]) null));
        assertFalse(Strings.isAnyEmpty(new String[] {}));
        assertTrue(Strings.isAnyEmpty(new String[] { "" }));

        assertTrue(Strings.isAnyBlank("a", "b", "c", " "));
        assertTrue(Strings.isAnyBlank("a", null, "c", "d"));
        assertFalse(Strings.isAnyBlank("a", "b", "c", "d"));
        assertTrue(Strings.isAnyBlank((String) null));
        assertFalse(Strings.isAnyBlank((String[]) null));
        assertFalse(Strings.isAnyBlank(new String[] {}));
        assertTrue(Strings.isAnyBlank(new String[] { "" }));
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
    public void testIsAsciiAlphanumericSpace() {
        assertTrue(Strings.isAsciiAlphanumericSpace("abc123"));
        assertTrue(Strings.isAsciiAlphanumericSpace("abc 123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc@123"));
        assertFalse(Strings.isAsciiAlphanumericSpace(null));
        assertFalse(Strings.isAsciiAlphanumericSpace(""));
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
    public void testIsAsciiControl() {
        assertTrue(Strings.isAsciiControl('\t'));
        assertTrue(Strings.isAsciiControl('\n'));
        assertTrue(Strings.isAsciiControl('\0'));
        assertFalse(Strings.isAsciiControl('a'));
        assertFalse(Strings.isAsciiControl('A'));
        assertFalse(Strings.isAsciiControl(' '));
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

    @Test
    public void testIsDigit() {
        assertTrue(Strings.isDigit('0'));
        assertTrue(Strings.isDigit('9'));
        assertFalse(Strings.isDigit('a'));
        assertFalse(Strings.isDigit('A'));
        assertFalse(Strings.isDigit(' '));
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
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("abc"));
        assertTrue(Strings.isNotEmpty("  abc  "));
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

    // isValidEmailAddress



    // C-001: long dotted / escaped inputs no longer overflow the stack
    @Test
    public void testC001_emailValidationAndFindingDoNotOverflowTheStack() throws Exception {
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("user@" + "a.".repeat(5000) + "com")));
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("a.".repeat(5000) + "a@x.y")));
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("\"" + "\\a".repeat(5000) + "\"@x.y")));
        assertTrue(onSmallStack(() -> Strings.isValidEmailAddress("user@[x:" + "a".repeat(5000) + "]")));
        assertEquals(Collections.emptyList(), onSmallStack(() -> Strings.findAllEmailAddresses("see " + "a.".repeat(5000) + " end")));
        final String longDomain = "user@" + "a.".repeat(5000) + "com";
        assertEquals(longDomain, onSmallStack(() -> Strings.findFirstEmailAddress("mail " + longDomain + " now")));
        assertEquals("user@a.b.c", Strings.findFirstEmailAddress("user@a.b.c."));
        assertEquals("user@example.com", Strings.findFirstEmailAddress("Email me at user@example.com."));
        // "!x@y.z" directly follows a '.', so it is a continuation of the previous token, not a separate address (C-436)
        assertEquals(Arrays.asList("a@b.c"), Strings.findAllEmailAddresses("a@b.c.!x@y.z"));
    }



    // C-002: the general address literal is a top-level alternative of the bracketed domain
    @Test
    public void testC002_generalAddressLiteral() {
        assertTrue(Strings.isValidEmailAddress("user@[IPv6:2001:db8::1]"));
        assertTrue(Strings.isValidEmailAddress("a@[x:abc]"));
        assertTrue(Strings.isValidEmailAddress("user@[127.0.0.1]"));
        assertFalse(Strings.isValidEmailAddress("user@[1.2.3.x:abc]"));
        assertFalse(Strings.isValidEmailAddress("user@[1.2.3.4:x]"));
        assertFalse(Strings.isValidEmailAddress("user@[256.1.1.1]"));
        assertFalse(Strings.isValidEmailAddress("user@[x:]"));
        assertFalse(Strings.isValidEmailAddress("a@[x:a]b]"));
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
    public void testC547_quotedLocalPartWhitespaceMustBeAQuotedPair() {
        assertTrue(Strings.isValidEmailAddress("\"a\\ b\"@x.y"));
        assertTrue(Strings.isValidEmailAddress("\"a\\\tb\"@x.y"));
        assertFalse(Strings.isValidEmailAddress("\"a b\"@x.y"));
        assertFalse(Strings.isValidEmailAddress("\"a\tb\"@x.y"));
        assertFalse(Strings.isValidEmailAddress("\" \"@example.org"));
        assertTrue(Strings.isValidEmailAddress("\"quoted.user\"@example.com"));
        assertTrue(Strings.isValidEmailAddress("\"a@b\"@x.y"));
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

    // isValidJavaIdentifier

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
    public void testIsValidJavaIdentifier_ContextualKeywords() {
        // JLS 17 §3.9 contextual keywords that are NOT excluded from TypeIdentifier: all legal type names.
        for (final String s : new String[] { "when", "module", "open", "requires", "exports", "opens", "to", "uses", "provides", "with", "transitive" }) {
            assertTrue(Strings.isValidJavaIdentifier(s), s);
            assertTrue(Strings.isValidJavaTypeIdentifier(s), s);
            assertFalse(Strings.isJavaKeyword(s), s);
        }

        assertFalse(Strings.isValidJavaIdentifier("non-sealed")); // fails the character test, not the list
        assertFalse(Strings.isValidJavaIdentifier("_")); // a keyword since Java 9
        assertFalse(Strings.isValidJavaIdentifier("class"));
        assertFalse(Strings.isValidJavaIdentifier("true"));
        assertFalse(Strings.isValidJavaIdentifier(""));
        assertFalse(Strings.isValidJavaIdentifier(null));

        // Everything a keyword/character test rejects is rejected by the type-name predicate too.
        assertFalse(Strings.isValidJavaTypeIdentifier("non-sealed"));
        assertFalse(Strings.isValidJavaTypeIdentifier("_"));
        assertFalse(Strings.isValidJavaTypeIdentifier("class"));
        assertFalse(Strings.isValidJavaTypeIdentifier("true"));
        assertFalse(Strings.isValidJavaTypeIdentifier(""));
        assertFalse(Strings.isValidJavaTypeIdentifier(null));
    }

    @Test
    public void testIsValidJavaIdentifier_Jls17RestrictedIdentifiers() {
        for (final String s : RESTRICTED) {
            // JLS 3.8 excludes the five from TypeIdentifier ONLY, so they are ordinary Identifiers: a field,
            // parameter, method or package segment may be named "record" or "var". Only a TYPE may not.
            assertTrue(Strings.isValidJavaIdentifier(s), s);
            assertFalse(Strings.isValidJavaTypeIdentifier(s), s);
            // Neither predicate leans on SourceVersion for this: none of the five is a keyword.
            assertFalse(Strings.isJavaKeyword(s), s + " is not a keyword");
            // The match is exact, so a name that merely contains one is still a legal type name.
            assertTrue(Strings.isValidJavaTypeIdentifier(s + "1"), s + "1");
            assertTrue(Strings.isValidJavaTypeIdentifier("_" + s), "_" + s);
            assertTrue(Strings.isValidJavaTypeIdentifier("my" + Strings.capitalize(s)), "my" + s);
            // ... and case-sensitive, like javac.
            assertTrue(Strings.isValidJavaTypeIdentifier(Strings.capitalize(s)), Strings.capitalize(s));
            assertTrue(Strings.isValidJavaTypeIdentifier(s.toUpperCase(java.util.Locale.ROOT)), s.toUpperCase(java.util.Locale.ROOT));
        }
    }

    @Test
    public void testIsValidJavaIdentifier_RestrictedAsCharSequence() {
        for (final String s : RESTRICTED) {
            assertTrue(Strings.isValidJavaIdentifier(new StringBuilder(s)), s);
            assertTrue(Strings.isValidJavaIdentifier(java.nio.CharBuffer.wrap(s)), s);
            assertFalse(Strings.isValidJavaTypeIdentifier(new StringBuilder(s)), s);
            assertFalse(Strings.isValidJavaTypeIdentifier(java.nio.CharBuffer.wrap(s)), s);
        }
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

    // isValidUrl

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
    public void testIsValidUrl_emptyPortDelimiterAndUserInfoAcceptedOutsideFileScheme() {
        // RFC 3986 allows an empty port, so http/https/ftp accept an authority ending in ':'.
        assertTrue(Strings.isValidUrl("http://host:/"));
        assertTrue(Strings.isValidUrl("https://host:"));
        assertTrue(Strings.isValidUrl("ftp://host:/"));
        assertTrue(Strings.isValidHttpUrl("http://host:/"));

        // ... which means a Windows drive letter is NOT rejected outside the file scheme.
        assertTrue(Strings.isValidUrl("http://C:/path"));

        // The file scheme rejects both, which is what the extra guard exists for.
        assertFalse(Strings.isValidUrl("file://host:/"));
        assertFalse(Strings.isValidUrl("file://host:"));
        assertFalse(Strings.isValidUrl("file://C:/path"));

        // User-info is likewise accepted for http/https/ftp and rejected for file.
        assertTrue(Strings.isValidUrl("http://user:pw@host/"));
        assertTrue(Strings.isValidHttpUrl("http://user:pw@host/"));
        assertFalse(Strings.isValidUrl("file://user@host/path"));

        // A valid file URL with no authority still passes.
        assertTrue(Strings.isValidUrl("file:///C:/Users/doc.txt"));
    }



    // C-009: documented host rules of isValidUrl / isValidHttpUrl
    @Test
    public void testC009_isValidUrlHostRules() {
        assertFalse(Strings.isValidUrl("https://例え.jp/"));
        assertTrue(Strings.isValidUrl("https://xn--r8jz45g.jp/"));
        assertTrue(Strings.isValidUrl("http://example.com/é"));
        assertTrue(Strings.isValidUrl("http://example.com:0/"));
        assertTrue(Strings.isValidHttpUrl("http://xn--r8jz45g.jp/"));
        assertFalse(Strings.isValidHttpUrl("http://例え.jp/"));
    }

    // isWrappedWith

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
    public void testIsWrappedWith_agreesWithThreeArgFormAndOldDivisionForm() {
        final String[] strs = { null, "", "a", "aa", "aaa", "aaaa", "aaaaa", "'x'", "''", "'", "ab", "abab", "ababa", "xayax" };
        final String[] fixes = { "a", "aa", "'", "ab", "x", "aba" };

        for (final String s : strs) {
            for (final String f : fixes) {
                final boolean actual = Strings.isWrappedWith(s, f);
                // the 3-arg sibling, which always used the subtraction form
                assertEquals(Strings.isWrappedWith(s, f, f), actual, "str=" + s + " fix=" + f);
                // the previous division form must agree too (the change is a pure refactor)
                final boolean oldForm = s != null && s.length() / 2 >= f.length() && s.startsWith(f) && s.endsWith(f);
                assertEquals(oldForm, actual, "old-vs-new str=" + s + " fix=" + f);
            }
        }
    }



    // O-2 : isWrappedWith(String, String)
    @Test
    public void testIsWrappedWith_documentedExamples() {
        assertTrue(Strings.isWrappedWith("'hello'", "'"));
        assertTrue(Strings.isWrappedWith("--comment--", "--"));
        assertFalse(Strings.isWrappedWith("hello", "'"));
        assertFalse(Strings.isWrappedWith("aaa", "aa")); // the matches overlap
        assertTrue(Strings.isWrappedWith("\"text\"", "\""));
        assertFalse(Strings.isWrappedWith("'hello\"", "'"));
        assertFalse(Strings.isWrappedWith(null, "'"));
    }

    @Test
    public void testIsWrappedWith_lengthBoundary() {
        // exactly 2 * prefixSuffix.length() is the smallest wrapped string
        assertTrue(Strings.isWrappedWith("aaaa", "aa"));
        assertFalse(Strings.isWrappedWith("aaa", "aa"));
        assertTrue(Strings.isWrappedWith("aaaaa", "aa"));
        assertTrue(Strings.isWrappedWith("''", "'"));
        assertFalse(Strings.isWrappedWith("'", "'"));
        assertFalse(Strings.isWrappedWith("", "x"));
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
    public void testIsWrappedWith_rejectsEmptyPrefixSuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("abc", null));
    }

    @Test
    public void testNullSafety() {
        assertDoesNotThrow(() -> {
            Strings.isEmpty(null);
            Strings.isBlank(null);
            Strings.trim(null);
            Strings.toLowerCase(null);
            Strings.toUpperCase(null);
            Strings.contains(null, "test");
            Strings.indexOf(null, "test");
            Strings.split(null, ",");
            Strings.replaceAll((String) null, "a", "b");
        });
    }

    @Test
    public void testParseUrlQuery_nullHostileMapTarget() {
        assertThrows(NullPointerException.class, () -> Strings.parseUrlQuery("a=1&flag", java.util.Hashtable.class));
        assertThrows(NullPointerException.class, () -> Strings.parseUrlQuery("flag", StandardCharsets.UTF_8, java.util.Hashtable.class));
        assertEquals("1", ((Map<?, ?>) Strings.parseUrlQuery("a=1", java.util.Hashtable.class)).get("a"));
        assertTrue(Strings.parseUrlQueryToMultimap("a=1&flag").containsKey("flag"));
    }

    @Test
    public void testC537_parseUrlQueryLenientDelegates() {
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", (Charset) null));
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", Charsets.UTF_8));
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", Map.class));
        assertEquals(Map.of("a", "%zz"), Strings.parseUrlQueryLenient("a=%zz", (Charset) null, Map.class));
        assertEquals(Map.of("a", "b c"), Strings.parseUrlQueryLenient("a=b+c", Charsets.UTF_8, Map.class));
        assertTrue(Strings.parseUrlQueryLenient("", Map.class).isEmpty());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQueryLenient("", (Class<?>) null)).getMessage().contains("targetType"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQueryLenient("a=1", Charsets.UTF_8, (Class<?>) null)).getMessage()
                .contains("targetType"));
    }

    @Test
    public void testStringBuilderInput() {
        StringBuilder sb = new StringBuilder("test");
        assertTrue(Strings.isNotEmpty(sb));
        assertFalse(Strings.isBlank(sb));
        assertEquals(4, Strings.lengthOfCommonPrefix(sb, "test"));
    }

    @Test
    public void testUnicodeCharacters() {
        String unicode = "Hello 世界";

        assertTrue(Strings.isNotEmpty(unicode));
        assertFalse(Strings.isAsciiPrintable(unicode));
        assertTrue(Strings.contains(unicode, "世界"));
    }

    @Test
    public void testUrlDecode() {
        Map<String, String> decoded = Strings.parseUrlQuery(Strings.encodeUrlQuery("a=b&c=d"));
        assertEquals(Map.of("a", "b", "c", "d"), decoded);
        assertEquals(Map.of(), Strings.parseUrlQuery(null));

        // A String containing '=' is an ALREADY-ENCODED query and is appended verbatim, so the legacy ';'
        // separator is preserved rather than normalized to '&'. Re-splitting and re-encoding such a string
        // escaped its existing escapes a second time ("q=a%20b" -> "q=a%2520b"), which is why the verbatim
        // rule exists; the second assertion pins that. parseUrlQuery accepts both separators, so the
        // round-trip on the last line still holds.
        assertEquals("a=1;b=2", Strings.encodeUrlQuery("a=1;b=2"));
        assertEquals("q=a%20b", Strings.encodeUrlQuery("q=a%20b"));
        assertEquals(Map.of("a", "1", "b", "2"), Strings.parseUrlQuery("a=1;b=2"));
        assertEquals(Map.of("a", "1", "b", "2"), Strings.parseUrlQuery(Strings.encodeUrlQuery("a=1;b=2")));

        assertEquals("a=1&flag", Strings.encodeUrlQuery("a=1&flag"));
        final Map<String, String> flagged = Strings.parseUrlQuery("a=1&flag");
        assertEquals("1", flagged.get("a"));
        assertNull(flagged.get("flag"));
        assertEquals("a=1&flag", Strings.encodeUrlQuery("a=1&flag"));
        assertEquals(flagged, Strings.parseUrlQuery(Strings.encodeUrlQuery("a=1&flag")));
    }

    @Test
    public void testUrlDecode_MultimapPreservesRepeatedParameters() {
        final ListMultimap<String, String> result = Strings.parseUrlQueryToMultimap("tag=java&tag=url&flag");

        assertEquals(List.of("java", "url"), result.get("tag"));
        assertEquals(1, result.get("flag").size());
        assertNull(result.get("flag").get(0));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQueryToMultimap("tag=%C3"));
        assertEquals("�", Strings.parseUrlQueryToMultimapLenient("tag=%C3").get("tag").get(0));
        assertEquals(List.of("java", "url"), Strings.parseUrlQueryToMultimapLenient("tag=java&tag=url").get("tag"));
        assertEquals("�", Strings.parseUrlQueryToMultimapLenient("tag=%C3", StandardCharsets.UTF_8).get("tag").get(0));
    }

    @Test
    public void testUrlDecode_StructuralContract() {
        final Map<String, String> result = Strings.parseUrlQuery("a=1;a=2&flag&&trim = value ");

        assertEquals("2", result.get("a"));
        assertTrue(result.containsKey("flag"));
        assertNull(result.get("flag"));
        assertEquals(" value ", result.get("trim "));
        assertEquals(3, result.size());

        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("bad=%ZZ"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("incomplete=%A"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("unicodeHex=%１２"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("invalidUtf8=%C3"));

        final Map<String, String> lenient = Strings.parseUrlQueryLenient("bad=%ZZ&incomplete=%A&unicodeHex=%１２&invalidUtf8=%C3");
        assertEquals("%ZZ", lenient.get("bad"));
        assertEquals("%A", lenient.get("incomplete"));
        assertEquals("%１２", lenient.get("unicodeHex"));
        assertEquals("�", lenient.get("invalidUtf8"));
    }

    @Test
    public void testUrlDecode_WithCharset() {
        Map<String, String> result = Strings.parseUrlQuery("name=test+data", java.nio.charset.StandardCharsets.UTF_8);
        assertNotNull(result);
        assertEquals("test data", result.get("name"));
        assertEquals("é", Strings.parseUrlQuery("name=%C3%A9", (Charset) null).get("name"));
        assertEquals(Map.of(), Strings.parseUrlQuery((String) null, (Charset) null));
    }

    @Test
    public void testUrlDecode_WithCharsetAndClass() {
        java.util.Map<String, String> result = Strings.parseUrlQuery("a=1&b=2", java.nio.charset.StandardCharsets.UTF_8, java.util.Map.class);
        assertNotNull(result);
        assertEquals("1", result.get("a"));
    }

    @Test
    public void testUrlDecode_WithCharsetAndClass_Null() {
        java.util.Map<String, String> result = Strings.parseUrlQuery((String) null, java.nio.charset.StandardCharsets.UTF_8, java.util.Map.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(Map.of("name", "é"), Strings.parseUrlQuery("name=%C3%A9", null, java.util.Map.class));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery((String) null, null, (Class<Object>) null));
    }

    @Test
    public void testUrlDecode_WithClass() {
        java.util.Map<String, String> result = Strings.parseUrlQuery("a=1&b=2", java.util.Map.class);
        assertNotNull(result);
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
    }

    @Test
    public void testUrlDecode_WithClass_Null() {
        java.util.Map<String, String> result = Strings.parseUrlQuery((String) null, java.util.Map.class);
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery((String) null, (Class<Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseUrlQuery("", String.class));
    }

    @Test
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }

    @Test
    public void testUuid_Unique() {
        String uuid1 = Strings.uuid();
        String uuid2 = Strings.uuid();
        assertFalse(uuid1.equals(uuid2));
    }

    @Test
    public void testUuidWithoutHyphens() {
        String result = Strings.uuidWithoutHyphens();
        assertNotNull(result);
        assertEquals(32, result.length());
        assertFalse(result.contains("-"));
        // Should generate unique values
        assertNotEquals(result, Strings.uuidWithoutHyphens());
    }

    @Test
    public void testUuidWithoutHyphens_matchesCanonicalHexLayout() {
        final String hex = Strings.uuidWithoutHyphens();
        final String canonical = hex.substring(0, 8) + "-" + hex.substring(8, 12) + "-" + hex.substring(12, 16) + "-" + hex.substring(16, 20) + "-"
                + hex.substring(20);
        final UUID parsed = UUID.fromString(canonical);
        assertEquals(hex, parsed.toString().replace("-", ""));
    }

}
