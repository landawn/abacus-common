package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.StrUtil;

@Tag("2025")
public class Strings2025Test extends TestBase {

    private static final String EMPTY = "";
    private static final String SPACE = " ";
    private static final String WHITESPACE = "   \t\n\r  ";
    private static final String ABC = "abc";
    private static final String ABC_UPPER = "ABC";
    private static final String HELLO_WORLD = "Hello World";

    @Test
    @DisplayName("Test uuid() generates valid UUID")
    public void testUuid() {
        String uuid = Strings.uuid();
        assertNotNull(uuid);
        assertEquals(36, uuid.length());
        assertTrue(uuid.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}"));
    }

    @Test
    @DisplayName("Test uuid() generates unique UUIDs")
    public void testUuid_Unique() {
        String uuid1 = Strings.uuid();
        String uuid2 = Strings.uuid();
        assertFalse(uuid1.equals(uuid2));
    }

    @Test
    @DisplayName("Test guid() generates valid GUID")
    public void testGuid() {
        String guid = Strings.guid();
        assertNotNull(guid);
        assertEquals(32, guid.length());
        assertFalse(guid.contains("-"));
        assertTrue(guid.matches("[a-f0-9]{32}"));
    }

    @Test
    @DisplayName("Test guid() generates unique GUIDs")
    public void testGuid_Unique() {
        String guid1 = Strings.guid();
        String guid2 = Strings.guid();
        assertFalse(guid1.equals(guid2));
    }

    @Test
    @DisplayName("Test valueOf() with null")
    public void testValueOf_Null() {
        assertNull(Strings.valueOf(null));
    }

    @Test
    @DisplayName("Test valueOf() with empty array")
    public void testValueOf_EmptyArray() {
        assertEquals("", Strings.valueOf(new char[0]));
    }

    @Test
    @DisplayName("Test valueOf() with char array")
    public void testValueOf_CharArray() {
        assertEquals("abc", Strings.valueOf(new char[] { 'a', 'b', 'c' }));
        assertEquals("Hello World", Strings.valueOf("Hello World".toCharArray()));
    }

    @Test
    @DisplayName("Test isValidJavaIdentifier() with valid identifiers")
    public void testIsValidJavaIdentifier_Valid() {
        assertTrue(Strings.isValidJavaIdentifier("variable"));
        assertTrue(Strings.isValidJavaIdentifier("_var"));
        assertTrue(Strings.isValidJavaIdentifier("$var"));
        assertTrue(Strings.isValidJavaIdentifier("var123"));
        assertTrue(Strings.isValidJavaIdentifier("MAX_VALUE"));
        assertTrue(Strings.isValidJavaIdentifier("class"));
    }

    @Test
    @DisplayName("Test isValidJavaIdentifier() with invalid identifiers")
    public void testIsValidJavaIdentifier_Invalid() {
        assertFalse(Strings.isValidJavaIdentifier("123var"));
        assertFalse(Strings.isValidJavaIdentifier("var-name"));
        assertFalse(Strings.isValidJavaIdentifier("var name"));
        assertFalse(Strings.isValidJavaIdentifier(null));
        assertFalse(Strings.isValidJavaIdentifier(""));
        assertFalse(Strings.isValidJavaIdentifier("  "));
    }

    @Test
    @DisplayName("Test isKeyword() with Java keywords")
    public void testIsKeyword_Valid() {
        assertTrue(Strings.isKeyword("class"));
        assertTrue(Strings.isKeyword("public"));
        assertTrue(Strings.isKeyword("if"));
        assertTrue(Strings.isKeyword("return"));
        assertTrue(Strings.isKeyword("void"));
        assertTrue(Strings.isKeyword("abstract"));
    }

    @Test
    @DisplayName("Test isKeyword() with non-keywords")
    public void testIsKeyword_Invalid() {
        assertFalse(Strings.isKeyword("Class"));
        assertFalse(Strings.isKeyword("hello"));
        assertFalse(Strings.isKeyword("myVariable"));
        assertFalse(Strings.isKeyword(null));
        assertFalse(Strings.isKeyword(""));
    }

    @Test
    @DisplayName("Test isValidEmailAddress() with valid emails")
    public void testIsValidEmailAddress_Valid() {
        assertTrue(Strings.isValidEmailAddress("test@example.com"));
        assertTrue(Strings.isValidEmailAddress("user.name@example.co.uk"));
        assertTrue(Strings.isValidEmailAddress("user+tag@example.com"));
        assertTrue(Strings.isValidEmailAddress("admin@domain.org"));
    }

    @Test
    @DisplayName("Test isValidEmailAddress() with invalid emails")
    public void testIsValidEmailAddress_Invalid() {
        assertFalse(Strings.isValidEmailAddress("@example.com"));
        assertFalse(Strings.isValidEmailAddress("test@"));
        assertFalse(Strings.isValidEmailAddress("test"));
        assertFalse(Strings.isValidEmailAddress("test.example.com"));
        assertFalse(Strings.isValidEmailAddress(null));
        assertFalse(Strings.isValidEmailAddress(""));
    }

    @Test
    @DisplayName("Test isValidUrl() with valid URLs")
    public void testIsValidUrl_Valid() {
        assertTrue(Strings.isValidUrl("http://example.com"));
        assertTrue(Strings.isValidUrl("https://example.com"));
        assertTrue(Strings.isValidUrl("ftp://example.com"));
        assertTrue(Strings.isValidUrl("http://example.com:8080/path"));
        assertTrue(Strings.isValidUrl("file://C:/Users/test.txt"));
    }

    @Test
    @DisplayName("Test isValidUrl() with invalid URLs")
    public void testIsValidUrl_Invalid() {
        assertFalse(Strings.isValidUrl("not a url"));
        assertFalse(Strings.isValidUrl("www.example.com"));
        assertFalse(Strings.isValidUrl(null));
        assertFalse(Strings.isValidUrl(""));
    }

    @Test
    @DisplayName("Test isValidHttpUrl() with valid HTTP URLs")
    public void testIsValidHttpUrl_Valid() {
        assertTrue(Strings.isValidHttpUrl("http://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com"));
        assertTrue(Strings.isValidHttpUrl("https://example.com:8443/path"));
        assertTrue(Strings.isValidHttpUrl("http://localhost:8080"));
    }

    @Test
    @DisplayName("Test isValidHttpUrl() with invalid HTTP URLs")
    public void testIsValidHttpUrl_Invalid() {
        assertFalse(Strings.isValidHttpUrl("ftp://example.com")); // not HTTP
        assertFalse(Strings.isValidHttpUrl("file:///C:/doc.txt")); // not HTTP
        assertFalse(Strings.isValidHttpUrl("www.example.com"));
        assertFalse(Strings.isValidHttpUrl(null));
        assertFalse(Strings.isValidHttpUrl(""));
    }

    @Test
    @DisplayName("Test isEmpty() with various inputs")
    public void testIsEmpty() {
        assertTrue(Strings.isEmpty(null));
        assertTrue(Strings.isEmpty(""));
        assertFalse(Strings.isEmpty(" "));
        assertFalse(Strings.isEmpty("abc"));
        assertFalse(Strings.isEmpty("  abc  "));
        assertTrue(Strings.isEmpty(new StringBuilder()));
        assertFalse(Strings.isEmpty(new StringBuilder("test")));
    }

    @Test
    @DisplayName("Test isBlank() with various inputs")
    public void testIsBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank(" "));
        assertTrue(Strings.isBlank("   \t\n\r  "));
        assertTrue(Strings.isBlank("\t"));
        assertTrue(Strings.isBlank("\n"));
        assertFalse(Strings.isBlank("abc"));
        assertFalse(Strings.isBlank("  abc  "));
    }

    @Test
    @DisplayName("Test isNotEmpty() with various inputs")
    public void testIsNotEmpty() {
        assertFalse(Strings.isNotEmpty(null));
        assertFalse(Strings.isNotEmpty(""));
        assertTrue(Strings.isNotEmpty(" "));
        assertTrue(Strings.isNotEmpty("abc"));
        assertTrue(Strings.isNotEmpty("  abc  "));
    }

    @Test
    @DisplayName("Test isNotBlank() with various inputs")
    public void testIsNotBlank() {
        assertFalse(Strings.isNotBlank(null));
        assertFalse(Strings.isNotBlank(""));
        assertFalse(Strings.isNotBlank(" "));
        assertFalse(Strings.isNotBlank("   \t\n\r  "));
        assertTrue(Strings.isNotBlank("abc"));
        assertTrue(Strings.isNotBlank("  abc  "));
    }

    @Test
    @DisplayName("Test isAllEmpty() with two arguments")
    public void testIsAllEmpty_TwoArgs() {
        assertTrue(Strings.isAllEmpty(null, null));
        assertTrue(Strings.isAllEmpty("", ""));
        assertTrue(Strings.isAllEmpty(null, ""));
        assertTrue(Strings.isAllEmpty("", null));
        assertFalse(Strings.isAllEmpty("abc", ""));
        assertFalse(Strings.isAllEmpty("", "xyz"));
        assertFalse(Strings.isAllEmpty("abc", "xyz"));
        assertFalse(Strings.isAllEmpty(" ", ""));
    }

    @Test
    @DisplayName("Test isAllEmpty() with three arguments")
    public void testIsAllEmpty_ThreeArgs() {
        assertTrue(Strings.isAllEmpty(null, null, null));
        assertTrue(Strings.isAllEmpty("", "", ""));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertFalse(Strings.isAllEmpty("abc", "", ""));
        assertFalse(Strings.isAllEmpty("", "xyz", ""));
        assertFalse(Strings.isAllEmpty("", "", "123"));
    }

    @Test
    @DisplayName("Test isAllEmpty() with varargs")
    public void testIsAllEmpty_VarArgs() {
        assertTrue(Strings.isAllEmpty());
        assertTrue(Strings.isAllEmpty((CharSequence[]) null));
        assertTrue(Strings.isAllEmpty(null, "", null));
        assertFalse(Strings.isAllEmpty(null, "foo", ""));
        assertFalse(Strings.isAllEmpty("", "bar", null));
        assertFalse(Strings.isAllEmpty(" ", "", null));
    }

    @Test
    @DisplayName("Test isAllEmpty() with Iterable")
    public void testIsAllEmpty_Iterable() {
        assertTrue(Strings.isAllEmpty((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllEmpty(new ArrayList<>()));
        assertTrue(Strings.isAllEmpty(Arrays.asList(null, "", null)));
        assertFalse(Strings.isAllEmpty(Arrays.asList("", "abc", "")));
        assertFalse(Strings.isAllEmpty(Arrays.asList("abc")));
    }

    @Test
    @DisplayName("Test isAllBlank() with two arguments")
    public void testIsAllBlank_TwoArgs() {
        assertTrue(Strings.isAllBlank(null, null));
        assertTrue(Strings.isAllBlank("", ""));
        assertTrue(Strings.isAllBlank("   ", "\t\n"));
        assertTrue(Strings.isAllBlank(null, "   "));
        assertFalse(Strings.isAllBlank("abc", "   "));
        assertFalse(Strings.isAllBlank("   ", "xyz"));
    }

    @Test
    @DisplayName("Test isAllBlank() with three arguments")
    public void testIsAllBlank_ThreeArgs() {
        assertTrue(Strings.isAllBlank(null, null, null));
        assertTrue(Strings.isAllBlank("", "", ""));
        assertTrue(Strings.isAllBlank("   ", "\t", "\n"));
        assertFalse(Strings.isAllBlank("abc", "   ", ""));
        assertFalse(Strings.isAllBlank("", "xyz", "   "));
    }

    @Test
    @DisplayName("Test isAllBlank() with varargs")
    public void testIsAllBlank_VarArgs() {
        assertTrue(Strings.isAllBlank());
        assertTrue(Strings.isAllBlank((CharSequence[]) null));
        assertTrue(Strings.isAllBlank(null, "", "  "));
        assertFalse(Strings.isAllBlank(null, "foo", "  "));
        assertFalse(Strings.isAllBlank("  ", "bar", null));
    }

    @Test
    @DisplayName("Test isAllBlank() with Iterable")
    public void testIsAllBlank_Iterable() {
        assertTrue(Strings.isAllBlank((Iterable<CharSequence>) null));
        assertTrue(Strings.isAllBlank(new ArrayList<>()));
        assertTrue(Strings.isAllBlank(Arrays.asList(null, "", "   ")));
        assertFalse(Strings.isAllBlank(Arrays.asList("   ", "abc", "")));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with two arguments")
    public void testIsAnyEmpty_TwoArgs() {
        assertTrue(Strings.isAnyEmpty(null, null));
        assertTrue(Strings.isAnyEmpty("", ""));
        assertTrue(Strings.isAnyEmpty("abc", ""));
        assertTrue(Strings.isAnyEmpty("", "xyz"));
        assertTrue(Strings.isAnyEmpty(null, "xyz"));
        assertFalse(Strings.isAnyEmpty("abc", "xyz"));
        assertFalse(Strings.isAnyEmpty("   ", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with three arguments")
    public void testIsAnyEmpty_ThreeArgs() {
        assertTrue(Strings.isAnyEmpty(null, null, null));
        assertTrue(Strings.isAnyEmpty("abc", "", "xyz"));
        assertTrue(Strings.isAnyEmpty("", "def", "xyz"));
        assertTrue(Strings.isAnyEmpty("abc", "def", null));
        assertFalse(Strings.isAnyEmpty("abc", "def", "xyz"));
        assertFalse(Strings.isAnyEmpty("   ", "def", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with varargs")
    public void testIsAnyEmpty_VarArgs() {
        assertFalse(Strings.isAnyEmpty());
        assertFalse(Strings.isAnyEmpty((CharSequence[]) null));
        assertTrue(Strings.isAnyEmpty(null, "foo"));
        assertTrue(Strings.isAnyEmpty("", "bar"));
        assertTrue(Strings.isAnyEmpty("bob", ""));
        assertTrue(Strings.isAnyEmpty("  bob  ", null));
        assertFalse(Strings.isAnyEmpty(" ", "bar"));
        assertFalse(Strings.isAnyEmpty("foo", "bar"));
    }

    @Test
    @DisplayName("Test isAnyEmpty() with Iterable")
    public void testIsAnyEmpty_Iterable() {
        assertFalse(Strings.isAnyEmpty((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyEmpty(new ArrayList<>()));
        assertTrue(Strings.isAnyEmpty(Arrays.asList("abc", "", "xyz")));
        assertFalse(Strings.isAnyEmpty(Arrays.asList("abc", "def", "xyz")));
    }

    @Test
    @DisplayName("Test isAnyBlank() with two arguments")
    public void testIsAnyBlank_TwoArgs() {
        assertTrue(Strings.isAnyBlank(null, null));
        assertTrue(Strings.isAnyBlank("", ""));
        assertTrue(Strings.isAnyBlank("   ", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "\t\n"));
        assertTrue(Strings.isAnyBlank(null, "xyz"));
        assertFalse(Strings.isAnyBlank("abc", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyBlank() with three arguments")
    public void testIsAnyBlank_ThreeArgs() {
        assertTrue(Strings.isAnyBlank(null, null, null));
        assertTrue(Strings.isAnyBlank("abc", "   ", "xyz"));
        assertTrue(Strings.isAnyBlank("", "def", "xyz"));
        assertTrue(Strings.isAnyBlank("abc", "def", null));
        assertFalse(Strings.isAnyBlank("abc", "def", "xyz"));
    }

    @Test
    @DisplayName("Test isAnyBlank() with varargs")
    public void testIsAnyBlank_VarArgs() {
        assertFalse(Strings.isAnyBlank());
        assertFalse(Strings.isAnyBlank((CharSequence[]) null));
        assertTrue(Strings.isAnyBlank(null, "foo"));
        assertTrue(Strings.isAnyBlank("", "bar"));
        assertTrue(Strings.isAnyBlank("  bob  ", null));
        assertTrue(Strings.isAnyBlank(" ", "bar"));
        assertFalse(Strings.isAnyBlank("foo", "bar"));
    }

    @Test
    @DisplayName("Test isAnyBlank() with Iterable")
    public void testIsAnyBlank_Iterable() {
        assertFalse(Strings.isAnyBlank((Iterable<CharSequence>) null));
        assertFalse(Strings.isAnyBlank(new ArrayList<>()));
        assertTrue(Strings.isAnyBlank(Arrays.asList("abc", "   ", "xyz")));
        assertFalse(Strings.isAnyBlank(Arrays.asList("abc", "def", "xyz")));
    }

    @Test
    @DisplayName("Test isWrappedWith() with same prefix/suffix")
    public void testIsWrappedWith_SamePrefixSuffix() {
        assertTrue(Strings.isWrappedWith("'hello'", "'"));
        assertTrue(Strings.isWrappedWith("\"text\"", "\""));
        assertTrue(Strings.isWrappedWith("--comment--", "--"));
        assertFalse(Strings.isWrappedWith("hello", "'"));
        assertFalse(Strings.isWrappedWith("'hello\"", "'"));
        assertFalse(Strings.isWrappedWith(null, "'"));
        assertFalse(Strings.isWrappedWith("''", "''"));
    }

    @Test
    @DisplayName("Test isWrappedWith() throws on empty prefix/suffix")
    public void testIsWrappedWith_EmptyPrefixSuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", ""));
    }

    @Test
    @DisplayName("Test isWrappedWith() with different prefix and suffix")
    public void testIsWrappedWith_DifferentPrefixSuffix() {
        assertTrue(Strings.isWrappedWith("<html>content</html>", "<html>", "</html>"));
        assertTrue(Strings.isWrappedWith("{data}", "{", "}"));
        assertTrue(Strings.isWrappedWith("[array]", "[", "]"));
        assertFalse(Strings.isWrappedWith("hello", "<", ">"));
        assertFalse(Strings.isWrappedWith("<hello", "<", ">"));
        assertFalse(Strings.isWrappedWith(null, "<", ">"));
    }

    @Test
    @DisplayName("Test isWrappedWith() throws on empty prefix or suffix")
    public void testIsWrappedWith_EmptyPrefixOrSuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "", ">"));
        assertThrows(IllegalArgumentException.class, () -> Strings.isWrappedWith("test", "<", ""));
    }

    @Test
    @DisplayName("Test defaultIfNull() returns value when not null")
    public void testDefaultIfNull_NotNull() {
        assertEquals("hello", Strings.defaultIfNull("hello", "default"));
        assertEquals("", Strings.defaultIfNull("", "default"));
        assertEquals("   ", Strings.defaultIfNull("   ", "default"));
    }

    @Test
    @DisplayName("Test defaultIfNull() returns default when null")
    public void testDefaultIfNull_Null() {
        assertEquals("default", Strings.defaultIfNull(null, "default"));
    }

    @Test
    @DisplayName("Test defaultIfNull() throws when default is null")
    public void testDefaultIfNull_NullDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfNull((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfNull() with Supplier")
    public void testDefaultIfNull_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfNull("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfNull(null, () -> "default"));
    }

    @Test
    @DisplayName("Test defaultIfNull() with Supplier throws when default is null")
    public void testDefaultIfNull_SupplierNullDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.<String> defaultIfNull(null, () -> null));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() returns value when not empty")
    public void testDefaultIfEmpty_NotEmpty() {
        assertEquals("hello", Strings.defaultIfEmpty("hello", "default"));
        assertEquals("   ", Strings.defaultIfEmpty("   ", "default"));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() returns default when empty")
    public void testDefaultIfEmpty_Empty() {
        assertEquals("default", Strings.defaultIfEmpty("", "default"));
        assertEquals("default", Strings.defaultIfEmpty(null, "default"));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() throws when default is empty")
    public void testDefaultIfEmpty_EmptyDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfEmpty((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfEmpty() with Supplier")
    public void testDefaultIfEmpty_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfEmpty("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty("", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfEmpty(null, () -> "default"));
    }

    @Test
    @DisplayName("Test defaultIfBlank() returns value when not blank")
    public void testDefaultIfBlank_NotBlank() {
        assertEquals("hello", Strings.defaultIfBlank("hello", "default"));
        assertEquals("  abc  ", Strings.defaultIfBlank("  abc  ", "default"));
    }

    @Test
    @DisplayName("Test defaultIfBlank() returns default when blank")
    public void testDefaultIfBlank_Blank() {
        assertEquals("default", Strings.defaultIfBlank("   ", "default"));
        assertEquals("default", Strings.defaultIfBlank("", "default"));
        assertEquals("default", Strings.defaultIfBlank(null, "default"));
    }

    @Test
    @DisplayName("Test defaultIfBlank() throws when default is blank")
    public void testDefaultIfBlank_BlankDefault() {
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, "  "));
        assertThrows(IllegalArgumentException.class, () -> Strings.defaultIfBlank((String) null, (String) null));
    }

    @Test
    @DisplayName("Test defaultIfBlank() with Supplier")
    public void testDefaultIfBlank_Supplier() {
        assertEquals("hello", Strings.<String> defaultIfBlank("hello", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank("   ", () -> "default"));
        assertEquals("default", Strings.<String> defaultIfBlank(null, () -> "default"));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with two arguments")
    public void testFirstNonEmpty_TwoArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world"));
        assertEquals("world", Strings.firstNonEmpty("", "world"));
        assertEquals("world", Strings.firstNonEmpty(null, "world"));
        assertEquals("", Strings.firstNonEmpty("", ""));
        assertEquals("", Strings.firstNonEmpty(null, null));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with three arguments")
    public void testFirstNonEmpty_ThreeArgs() {
        assertEquals("hello", Strings.firstNonEmpty("hello", "world", "!"));
        assertEquals("world", Strings.firstNonEmpty("", "world", "!"));
        assertEquals("!", Strings.firstNonEmpty("", "", "!"));
        assertEquals("!", Strings.firstNonEmpty(null, null, "!"));
        assertEquals("", Strings.firstNonEmpty("", "", ""));
        assertEquals("", Strings.firstNonEmpty(null, null, null));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with varargs")
    public void testFirstNonEmpty_VarArgs() {
        assertEquals("", Strings.firstNonEmpty());
        assertEquals("", Strings.firstNonEmpty((String[]) null));
        assertEquals("", Strings.firstNonEmpty(null, null, null));
        assertEquals(" ", Strings.firstNonEmpty(null, "", " "));
        assertEquals("abc", Strings.firstNonEmpty("abc"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty("", "xyz"));
        assertEquals("xyz", Strings.firstNonEmpty(null, "xyz", "abc"));
    }

    @Test
    @DisplayName("Test firstNonEmpty() with Iterable")
    public void testFirstNonEmpty_Iterable() {
        assertEquals("", Strings.firstNonEmpty((Iterable<String>) null));
        assertEquals("", Strings.firstNonEmpty(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonEmpty(Arrays.asList("", null, "hello")));
        assertEquals("", Strings.firstNonEmpty(Arrays.asList("", null, "")));
    }

    @Test
    @DisplayName("Test firstNonBlank() with two arguments")
    public void testFirstNonBlank_TwoArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world"));
        assertEquals("world", Strings.firstNonBlank("   ", "world"));
        assertEquals("world", Strings.firstNonBlank(null, "world"));
        assertEquals("", Strings.firstNonBlank("", ""));
        assertEquals("", Strings.firstNonBlank(null, null));
        assertEquals("", Strings.firstNonBlank("  ", "  "));
    }

    @Test
    @DisplayName("Test firstNonBlank() with three arguments")
    public void testFirstNonBlank_ThreeArgs() {
        assertEquals("hello", Strings.firstNonBlank("hello", "world", "!"));
        assertEquals("world", Strings.firstNonBlank("   ", "world", "!"));
        assertEquals("!", Strings.firstNonBlank("  ", "", "!"));
        assertEquals("!", Strings.firstNonBlank(null, null, "!"));
        assertEquals("", Strings.firstNonBlank("", "", ""));
    }

    @Test
    @DisplayName("Test firstNonBlank() with varargs")
    public void testFirstNonBlank_VarArgs() {
        assertEquals("", Strings.firstNonBlank());
        assertEquals("", Strings.firstNonBlank((String[]) null));
        assertEquals("abc", Strings.firstNonBlank(null, "  ", "abc"));
        assertEquals("", Strings.firstNonBlank(null, "", "   "));
    }

    @Test
    @DisplayName("Test firstNonBlank() with Iterable")
    public void testFirstNonBlank_Iterable() {
        assertEquals("", Strings.firstNonBlank((Iterable<String>) null));
        assertEquals("", Strings.firstNonBlank(new ArrayList<>()));
        assertEquals("hello", Strings.firstNonBlank(Arrays.asList("   ", null, "hello")));
        assertEquals("", Strings.firstNonBlank(Arrays.asList("  ", null, "")));
    }

    @Test
    @DisplayName("Test nullToEmpty()")
    public void testNullToEmpty() {
        assertEquals("", Strings.nullToEmpty((String) null));
        assertEquals("", Strings.nullToEmpty(""));
        assertEquals("abc", Strings.nullToEmpty("abc"));
        assertEquals("   ", Strings.nullToEmpty("   "));
    }

    @Test
    @DisplayName("Test nullToEmpty() with array")
    public void testNullToEmpty_Array() {
        String[] array = { null, "abc", null, "xyz" };
        Strings.nullToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);

        String[] emptyArray = {};
        Strings.nullToEmpty(emptyArray);
        assertArrayEquals(new String[] {}, emptyArray);
    }

    @Test
    @DisplayName("Test emptyToNull()")
    public void testEmptyToNull() {
        assertNull(Strings.emptyToNull(""));
        assertNull(Strings.emptyToNull((String) null));
        assertEquals("abc", Strings.emptyToNull("abc"));
        assertEquals("   ", Strings.emptyToNull("   "));
    }

    @Test
    @DisplayName("Test emptyToNull() with array")
    public void testEmptyToNull_Array() {
        String[] array = { "", "abc", "", "xyz" };
        Strings.emptyToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    @DisplayName("Test blankToEmpty()")
    public void testBlankToEmpty() {
        assertEquals("", Strings.blankToEmpty((String) null));
        assertEquals("", Strings.blankToEmpty(""));
        assertEquals("", Strings.blankToEmpty("   "));
        assertEquals("abc", Strings.blankToEmpty("abc"));
        assertEquals("  abc  ", Strings.blankToEmpty("  abc  "));
    }

    @Test
    @DisplayName("Test blankToEmpty() with array")
    public void testBlankToEmpty_Array() {
        String[] array = { null, "abc", "  ", "xyz" };
        Strings.blankToEmpty(array);
        assertArrayEquals(new String[] { "", "abc", "", "xyz" }, array);
    }

    @Test
    @DisplayName("Test blankToNull()")
    public void testBlankToNull() {
        assertNull(Strings.blankToNull((String) null));
        assertNull(Strings.blankToNull(""));
        assertNull(Strings.blankToNull("   "));
        assertEquals("abc", Strings.blankToNull("abc"));
        assertEquals("  abc  ", Strings.blankToNull("  abc  "));
    }

    @Test
    @DisplayName("Test blankToNull() with array")
    public void testBlankToNull_Array() {
        String[] array = { "  ", "abc", "", "xyz" };
        Strings.blankToNull(array);
        assertArrayEquals(new String[] { null, "abc", null, "xyz" }, array);
    }

    @Test
    @DisplayName("Test abbreviate() with normal cases")
    public void testAbbreviate() {
        assertEquals("abc", Strings.abbreviate("abc", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", 5));
        assertNull(Strings.abbreviate(null, 5));
        assertEquals("", Strings.abbreviate("", 5));
    }

    @Test
    @DisplayName("Test abbreviate() with custom marker")
    public void testAbbreviate_WithMarker() {
        assertEquals("abc", Strings.abbreviate("abc", "...", 5));
        assertEquals("ab...", Strings.abbreviate("abcdefg", "...", 5));
        assertEquals("abcd*", Strings.abbreviate("abcdefg", "*", 5));
        assertNull(Strings.abbreviate(null, "...", 5));
    }

    @Test
    @DisplayName("Test abbreviateMiddle()")
    public void testAbbreviateMiddle() {
        assertEquals("abc...xyz", Strings.abbreviateMiddle("abcdefghijklmnopqrstuvwxyz", "...", 9));
        assertEquals("abc", Strings.abbreviateMiddle("abc", "...", 10));
        assertNull(Strings.abbreviateMiddle(null, "...", 5));
    }

    @Test
    @DisplayName("Test center() with default space")
    public void testCenter() {
        assertEquals("  abc  ", Strings.center("abc", 7));
        assertEquals(" abc  ", Strings.center("abc", 6));
        assertEquals("abc", Strings.center("abc", 3));
        assertEquals("abc", Strings.center("abc", 2));
        assertEquals("   ", Strings.center(null, 3));
    }

    @Test
    @DisplayName("Test center() with custom pad char")
    public void testCenter_WithChar() {
        assertEquals("**abc**", Strings.center("abc", 7, '*'));
        assertEquals("*abc**", Strings.center("abc", 6, '*'));
        assertEquals("abc", Strings.center("abc", 3, '*'));
    }

    @Test
    @DisplayName("Test center() with custom pad string")
    public void testCenter_WithString() {
        assertEquals("--abc--", Strings.center("abc", 7, "--"));
        assertEquals("-abc--", Strings.center("abc", 6, "-"));
        assertEquals("abc", Strings.center("abc", 3, "-"));
        assertEquals("-----", Strings.center(null, 5, "-"));
    }

    @Test
    @DisplayName("Test padStart() with default space")
    public void testPadStart() {
        assertEquals("  abc", Strings.padStart("abc", 5));
        assertEquals("abc", Strings.padStart("abc", 3));
        assertEquals("abc", Strings.padStart("abc", 2));
        assertEquals("     ", Strings.padStart(null, 5));
    }

    @Test
    @DisplayName("Test padStart() with custom char")
    public void testPadStart_WithChar() {
        assertEquals("00abc", Strings.padStart("abc", 5, '0'));
        assertEquals("abc", Strings.padStart("abc", 3, '0'));
    }

    @Test
    @DisplayName("Test padStart() with custom string")
    public void testPadStart_WithString() {
        assertEquals("--abc", Strings.padStart("abc", 5, "--"));
        assertEquals("abc", Strings.padStart("abc", 3, "-"));
    }

    @Test
    @DisplayName("Test padEnd() with default space")
    public void testPadEnd() {
        assertEquals("abc  ", Strings.padEnd("abc", 5));
        assertEquals("abc", Strings.padEnd("abc", 3));
        assertEquals("abc", Strings.padEnd("abc", 2));
        assertEquals("     ", Strings.padEnd(null, 5));
    }

    @Test
    @DisplayName("Test padEnd() with custom char")
    public void testPadEnd_WithChar() {
        assertEquals("abc00", Strings.padEnd("abc", 5, '0'));
        assertEquals("abc", Strings.padEnd("abc", 3, '0'));
    }

    @Test
    @DisplayName("Test padEnd() with custom string")
    public void testPadEnd_WithString() {
        assertEquals("abc--", Strings.padEnd("abc", 5, "--"));
        assertEquals("abc", Strings.padEnd("abc", 3, "-"));
    }

    @Test
    @DisplayName("Test repeat() with char")
    public void testRepeat_Char() {
        assertEquals("aaa", Strings.repeat('a', 3));
        assertEquals("", Strings.repeat('a', 0));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat('a', -1));
    }

    @Test
    @DisplayName("Test repeat() with char and delimiter")
    public void testRepeat_CharWithDelimiter() {
        assertEquals("a,a,a", Strings.repeat('a', 3, ','));
        assertEquals("a", Strings.repeat('a', 1, ','));
        assertEquals("", Strings.repeat('a', 0, ','));
    }

    @Test
    @DisplayName("Test repeat() with string")
    public void testRepeat_String() {
        assertEquals("abcabcabc", Strings.repeat("abc", 3));
        assertEquals("", Strings.repeat("abc", 0));
        assertEquals("", Strings.repeat(null, 3));
        assertThrows(IllegalArgumentException.class, () -> Strings.repeat("abc", -1));
    }

    @Test
    @DisplayName("Test repeat() with string and delimiter")
    public void testRepeat_StringWithDelimiter() {
        assertEquals("abc,abc,abc", Strings.repeat("abc", 3, ","));
        assertEquals("abc", Strings.repeat("abc", 1, ","));
        assertEquals("", Strings.repeat("abc", 0, ","));
    }

    @Test
    @DisplayName("Test repeat() with prefix and suffix")
    public void testRepeat_WithPrefixSuffix() {
        assertEquals("[abc,abc,abc]", Strings.repeat("abc", 3, ",", "[", "]"));
        assertEquals("[abc]", Strings.repeat("abc", 1, ",", "[", "]"));
        assertEquals("[]", Strings.repeat("abc", 0, ",", "[", "]"));
    }

    @Test
    @DisplayName("Test getBytes() with default encoding")
    public void testGetBytes() {
        assertArrayEquals("abc".getBytes(), Strings.getBytes("abc"));
        assertNull(Strings.getBytes(null));
    }

    @Test
    @DisplayName("Test getBytes() with charset")
    public void testGetBytes_WithCharset() {
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Strings.getBytes("abc", StandardCharsets.UTF_8));
        assertNull(Strings.getBytes(null, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test getBytesUtf8()")
    public void testGetBytesUtf8() {
        assertArrayEquals("abc".getBytes(StandardCharsets.UTF_8), Strings.getBytesUtf8("abc"));
        assertNull(Strings.getBytesUtf8(null));
    }

    @Test
    @DisplayName("Test toCharArray()")
    public void testToCharArray() {
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, Strings.toCharArray("abc"));
        assertArrayEquals(new char[0], Strings.toCharArray(""));
        assertNull(Strings.toCharArray(null));
    }

    @Test
    @DisplayName("Test toLowerCase() with char")
    public void testToLowerCase_Char() {
        assertEquals('a', Strings.toLowerCase('A'));
        assertEquals('z', Strings.toLowerCase('Z'));
        assertEquals('a', Strings.toLowerCase('a'));
        assertEquals('1', Strings.toLowerCase('1'));
    }

    @Test
    @DisplayName("Test toLowerCase() with string")
    public void testToLowerCase_String() {
        assertEquals("abc", Strings.toLowerCase("ABC"));
        assertEquals("abc", Strings.toLowerCase("abc"));
        assertEquals("abc123", Strings.toLowerCase("ABC123"));
        assertNull(Strings.toLowerCase(null));
        assertEquals("", Strings.toLowerCase(""));
    }

    @Test
    @DisplayName("Test toLowerCase() with locale")
    public void testToLowerCase_WithLocale() {
        assertEquals("abc", Strings.toLowerCase("ABC", Locale.ENGLISH));
        assertNull(Strings.toLowerCase(null, Locale.ENGLISH));
    }

    @Test
    @DisplayName("Test toLowerCaseWithUnderscore()")
    public void testToLowerCaseWithUnderscore() {
        assertEquals("hello_world", Strings.toLowerCaseWithUnderscore("HelloWorld"));
        assertEquals("hello_world", Strings.toLowerCaseWithUnderscore("helloWorld"));
        assertEquals("abc", Strings.toLowerCaseWithUnderscore("abc"));
        assertNull(Strings.toLowerCaseWithUnderscore(null));
    }

    @Test
    @DisplayName("Test toUpperCase() with char")
    public void testToUpperCase_Char() {
        assertEquals('A', Strings.toUpperCase('a'));
        assertEquals('Z', Strings.toUpperCase('z'));
        assertEquals('A', Strings.toUpperCase('A'));
        assertEquals('1', Strings.toUpperCase('1'));
    }

    @Test
    @DisplayName("Test toUpperCase() with string")
    public void testToUpperCase_String() {
        assertEquals("ABC", Strings.toUpperCase("abc"));
        assertEquals("ABC", Strings.toUpperCase("ABC"));
        assertEquals("ABC123", Strings.toUpperCase("abc123"));
        assertNull(Strings.toUpperCase(null));
        assertEquals("", Strings.toUpperCase(""));
    }

    @Test
    @DisplayName("Test toUpperCase() with locale")
    public void testToUpperCase_WithLocale() {
        assertEquals("ABC", Strings.toUpperCase("abc", Locale.ENGLISH));
        assertNull(Strings.toUpperCase(null, Locale.ENGLISH));
    }

    @Test
    @DisplayName("Test toUpperCaseWithUnderscore()")
    public void testToUpperCaseWithUnderscore() {
        assertEquals("HELLO_WORLD", Strings.toUpperCaseWithUnderscore("HelloWorld"));
        assertEquals("HELLO_WORLD", Strings.toUpperCaseWithUnderscore("helloWorld"));
        assertEquals("ABC", Strings.toUpperCaseWithUnderscore("ABC"));
        assertNull(Strings.toUpperCaseWithUnderscore(null));
    }

    @Test
    @DisplayName("Test toCamelCase()")
    public void testToCamelCase() {
        assertEquals("helloWorld", Strings.toCamelCase("hello_world"));
        assertEquals("helloWorld", Strings.toCamelCase("HELLO_WORLD"));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world"));
        assertNull(Strings.toCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
    }

    @Test
    @DisplayName("Test toCamelCase() with custom split char")
    public void testToCamelCase_WithSplitChar() {
        assertEquals("helloWorld", Strings.toCamelCase("hello_world", '_'));
        assertEquals("helloWorld", Strings.toCamelCase("hello-world", '-'));
        assertNull(Strings.toCamelCase(null, '_'));
    }

    @Test
    @DisplayName("Test toPascalCase()")
    public void testToPascalCase() {
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world"));
        assertEquals("HelloWorld", Strings.toPascalCase("HELLO_WORLD"));
        assertEquals("HelloWorld", Strings.toPascalCase("hello-world"));
        assertNull(Strings.toPascalCase(null));
        assertEquals("", Strings.toPascalCase(""));
    }

    @Test
    @DisplayName("Test toPascalCase() with custom split char")
    public void testToPascalCase_WithSplitChar() {
        assertEquals("HelloWorld", Strings.toPascalCase("hello_world", '_'));
        assertEquals("HelloWorld", Strings.toPascalCase("hello-world", '-'));
        assertNull(Strings.toPascalCase(null, '_'));
    }

    @Test
    @DisplayName("Test swapCase() with char")
    public void testSwapCase_Char() {
        assertEquals('A', Strings.swapCase('a'));
        assertEquals('a', Strings.swapCase('A'));
        assertEquals('1', Strings.swapCase('1'));
    }

    @Test
    @DisplayName("Test swapCase() with string")
    public void testSwapCase_String() {
        assertEquals("ABC", Strings.swapCase("abc"));
        assertEquals("abc", Strings.swapCase("ABC"));
        assertEquals("AbC", Strings.swapCase("aBc"));
        assertNull(Strings.swapCase(null));
        assertEquals("", Strings.swapCase(""));
    }

    @Test
    @DisplayName("Test capitalize()")
    public void testCapitalize() {
        assertEquals("Abc", Strings.capitalize("abc"));
        assertEquals("Abc", Strings.capitalize("Abc"));
        assertEquals("ABC", Strings.capitalize("ABC"));
        assertNull(Strings.capitalize(null));
        assertEquals("", Strings.capitalize(""));
    }

    @Test
    @DisplayName("Test uncapitalize()")
    public void testUncapitalize() {
        assertEquals("abc", Strings.uncapitalize("Abc"));
        assertEquals("abc", Strings.uncapitalize("abc"));
        assertEquals("aBC", Strings.uncapitalize("ABC"));
        assertNull(Strings.uncapitalize(null));
        assertEquals("", Strings.uncapitalize(""));
    }

    @Test
    @DisplayName("Test capitalizeFully()")
    public void testCapitalizeFully() {
        assertEquals("Abc Def", Strings.capitalizeFully("abc def"));
        assertEquals("ABC DEF", Strings.capitalizeFully("ABC DEF"));
        assertNull(Strings.capitalizeFully(null));
        assertEquals("", Strings.capitalizeFully(""));
    }

    @Test
    @DisplayName("Test capitalizeFully() with delimiter")
    public void testCapitalizeFully_WithDelimiter() {
        assertEquals("Abc-Def", Strings.capitalizeFully("abc-def", "-"));
        assertEquals("ABC_DEF", Strings.capitalizeFully("ABC_DEF", "_"));
        assertNull(Strings.capitalizeFully(null, "-"));
    }

    @Test
    @DisplayName("Test capitalizeFully() with excluded words")
    public void testCapitalizeFully_WithExcludedWords() {
        assertEquals("The Quick Brown Fox", Strings.capitalizeFully("the quick brown fox", " ", "the"));
        assertEquals("Hello and Goodbye", Strings.capitalizeFully("hello and goodbye", " ", "and"));
    }

    @Test
    @DisplayName("Test capitalizeFully() with excluded words collection")
    public void testCapitalizeFully_WithExcludedWordsCollection() {
        List<String> excludedWords = Arrays.asList("the", "and", "or");
        assertEquals("The Quick Brown Fox", Strings.capitalizeFully("the quick brown fox", " ", excludedWords));
    }

    @Test
    @DisplayName("Test quoteEscaped() with default quote")
    public void testQuoteEscaped() {
        assertEquals("\\\"hello\\\"", Strings.quoteEscaped("\"hello\""));
        assertEquals("abc", Strings.quoteEscaped("abc"));
        assertNull(Strings.quoteEscaped(null));
    }

    @Test
    @DisplayName("Test quoteEscaped() with custom quote char")
    public void testQuoteEscaped_WithQuoteChar() {
        assertEquals("\\'hello\\'", Strings.quoteEscaped("'hello'", '\''));
        assertEquals("abc", Strings.quoteEscaped("abc", '\''));
        assertNull(Strings.quoteEscaped(null, '\''));
    }

    @Test
    @DisplayName("Test unicodeEscaped()")
    public void testUnicodeEscaped() {
        assertEquals("\\u0041", Strings.unicodeEscaped('A'));
        assertEquals("\\u0061", Strings.unicodeEscaped('a'));
        assertEquals("\\u0031", Strings.unicodeEscaped('1'));
    }

    @Test
    @DisplayName("Test normalizeSpace()")
    public void testNormalizeSpace() {
        assertEquals("a b c", Strings.normalizeSpace("a  b   c"));
        assertEquals("abc", Strings.normalizeSpace("abc"));
        assertEquals("a b", Strings.normalizeSpace("  a  b  "));
        assertNull(Strings.normalizeSpace(null));
        assertEquals("", Strings.normalizeSpace(""));
    }

    @Test
    @DisplayName("Test replaceAll()")
    public void testReplaceAll() {
        assertEquals("xbcxbc", Strings.replaceAll("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceAll("abc", "x", "y"));
        assertNull(Strings.replaceAll(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceAll() with fromIndex")
    public void testReplaceAll_WithFromIndex() {
        assertEquals("abcxbc", Strings.replaceAll("abcabc", 3, "a", "x"));
        assertEquals("abcabc", Strings.replaceAll("abcabc", 0, "x", "y"));
    }

    @Test
    @DisplayName("Test replaceFirst()")
    public void testReplaceFirst() {
        assertEquals("xbcabc", Strings.replaceFirst("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceFirst("abc", "x", "y"));
        assertNull(Strings.replaceFirst(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceFirst() with fromIndex")
    public void testReplaceFirst_WithFromIndex() {
        assertEquals("abcxbc", Strings.replaceFirst("abcabc", 3, "a", "x"));
        assertEquals("abcabc", Strings.replaceFirst("abcabc", 0, "x", "y"));
    }

    @Test
    @DisplayName("Test replaceOnce()")
    public void testReplaceOnce() {
        assertEquals("xbcabc", Strings.replaceOnce("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceOnce("abc", "x", "y"));
        assertNull(Strings.replaceOnce(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceLast()")
    public void testReplaceLast() {
        assertEquals("abcxbc", Strings.replaceLast("abcabc", "a", "x"));
        assertEquals("abc", Strings.replaceLast("abc", "x", "y"));
        assertNull(Strings.replaceLast(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replace() with max")
    public void testReplace_WithMax() {
        assertEquals("xbcxbc", Strings.replace("abcabc", 0, "a", "x", 2));
        assertEquals("xbcabc", Strings.replace("abcabc", 0, "a", "x", 1));
        assertEquals("abcabc", Strings.replace("abcabc", 0, "a", "x", 0));
    }

    @Test
    @DisplayName("Test replaceAllIgnoreCase()")
    public void testReplaceAllIgnoreCase() {
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcxbc", Strings.replaceAllIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceAllIgnoreCase(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replaceFirstIgnoreCase()")
    public void testReplaceFirstIgnoreCase() {
        assertEquals("xbcAbc", Strings.replaceFirstIgnoreCase("AbcAbc", "a", "x"));
        assertEquals("xbcabc", Strings.replaceFirstIgnoreCase("abcabc", "A", "x"));
        assertNull(Strings.replaceFirstIgnoreCase(null, "a", "x"));
    }

    @Test
    @DisplayName("Test replace() with fromIndex and toIndex")
    public void testReplace_WithIndices() {
        assertEquals("abXYZfg", Strings.replace("abcdefg", 2, 5, "XYZ"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replace(null, 0, 1, "X"));
    }

    @Test
    @DisplayName("Test replaceBetween()")
    public void testReplaceBetween() {
        assertEquals("abc[REPLACED]xyz", Strings.replaceBetween("abc[old]xyz", "[", "]", "REPLACED"));
        assertNull(Strings.replaceBetween(null, "[", "]", "X"));
    }

    @Test
    @DisplayName("Test replaceAfter()")
    public void testReplaceAfter() {
        assertEquals("abc:REPLACED", Strings.replaceAfter("abc:old", ":", "REPLACED"));
        assertNull(Strings.replaceAfter(null, ":", "X"));
    }

    @Test
    @DisplayName("Test replaceBefore()")
    public void testReplaceBefore() {
        assertEquals("REPLACED:xyz", Strings.replaceBefore("old:xyz", ":", "REPLACED"));
        assertNull(Strings.replaceBefore(null, ":", "X"));
    }

    @Test
    @DisplayName("Test removeStart()")
    public void testRemoveStart() {
        assertEquals("bc", Strings.removeStart("abc", "a"));
        assertEquals("abc", Strings.removeStart("abc", "x"));
        assertEquals("", Strings.removeStart("abc", "abc"));
        assertNull(Strings.removeStart(null, "a"));
    }

    @Test
    @DisplayName("Test removeStartIgnoreCase()")
    public void testRemoveStartIgnoreCase() {
        assertEquals("bc", Strings.removeStartIgnoreCase("abc", "A"));
        assertEquals("bc", Strings.removeStartIgnoreCase("Abc", "a"));
        assertNull(Strings.removeStartIgnoreCase(null, "a"));
    }

    @Test
    @DisplayName("Test removeEnd()")
    public void testRemoveEnd() {
        assertEquals("ab", Strings.removeEnd("abc", "c"));
        assertEquals("abc", Strings.removeEnd("abc", "x"));
        assertEquals("", Strings.removeEnd("abc", "abc"));
        assertEquals(null, Strings.removeEnd(null, "c"));
    }

    @Test
    @DisplayName("Test removeEndIgnoreCase()")
    public void testRemoveEndIgnoreCase() {
        assertEquals("ab", Strings.removeEndIgnoreCase("abc", "C"));
        assertEquals("ab", Strings.removeEndIgnoreCase("abC", "c"));
        assertNull(Strings.removeEndIgnoreCase(null, "c"));
    }

    @Test
    @DisplayName("Test removeAll() with char")
    public void testRemoveAll_Char() {
        assertEquals("bc", Strings.removeAll("abc", 'a'));
        assertEquals("", Strings.removeAll("aaa", 'a'));
        assertEquals("abc", Strings.removeAll("abc", 'x'));
        assertNull(Strings.removeAll(null, 'a'));
    }

    @Test
    @DisplayName("Test removeAll() with char and fromIndex")
    public void testRemoveAll_CharWithFromIndex() {
        assertEquals("abc", Strings.removeAll("abcaa", 3, 'a'));
        assertEquals("abc", Strings.removeAll("abc", 0, 'x'));
    }

    @Test
    @DisplayName("Test removeAll() with string")
    public void testRemoveAll_String() {
        assertEquals("cde", Strings.removeAll("ababcde", "ab"));
        assertEquals("abc", Strings.removeAll("abc", "xy"));
        assertNull(Strings.removeAll(null, "ab"));
    }

    @Test
    @DisplayName("Test removeAll() with string and fromIndex")
    public void testRemoveAll_StringWithFromIndex() {
        assertEquals("ababcde", Strings.removeAll("ababcde", 4, "ab"));
        assertEquals("abc", Strings.removeAll("abc", 0, "xy"));
    }

    @Test
    @DisplayName("Test split() with char delimiter")
    public void testSplit_Char() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ','));
        assertArrayEquals(new String[] { "abc" }, Strings.split("abc", ','));
        assertArrayEquals(new String[] {}, Strings.split(null, ','));
    }

    @Test
    @DisplayName("Test split() with char delimiter and trim")
    public void testSplit_CharWithTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ',', false));
    }

    @Test
    @DisplayName("Test split() with string delimiter")
    public void testSplit_String() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a::b::c", "::"));
        assertArrayEquals(new String[] {}, Strings.split(null, ","));
    }

    @Test
    @DisplayName("Test split() with string delimiter and trim")
    public void testSplit_StringWithTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split(" a , b , c ", ",", true));
        assertArrayEquals(new String[] { " a ", " b ", " c " }, Strings.split(" a , b , c ", ",", false));
    }

    @Test
    @DisplayName("Test split() with max")
    public void testSplit_WithMax() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.split("a,b,c", ",", 3));
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a,b,c" }, Strings.split("a,b,c", ",", 1));
    }

    @Test
    @DisplayName("Test split() with max and trim")
    public void testSplit_WithMaxAndTrim() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.split(" a , b,c ", ",", 2, true));
        assertArrayEquals(new String[] { " a ", " b,c " }, Strings.split(" a , b,c ", ",", 2, false));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with char")
    public void testSplitPreserveAllTokens_Char() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ','));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ','));
        assertArrayEquals(new String[] { "", "a" }, Strings.splitPreserveAllTokens(",a", ','));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with char and trim")
    public void testSplitPreserveAllTokens_CharWithTrim() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens(" a , b , c ", ',', true));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ',', true));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with string")
    public void testSplitPreserveAllTokens_String() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitPreserveAllTokens("a,b,c", ","));
        assertArrayEquals(new String[] { "a", "", "c" }, Strings.splitPreserveAllTokens("a,,c", ","));
    }

    @Test
    @DisplayName("Test splitPreserveAllTokens() with max")
    public void testSplitPreserveAllTokens_WithMax() {
        assertArrayEquals(new String[] { "a", "b,c" }, Strings.splitPreserveAllTokens("a,b,c", ",", 2));
        assertArrayEquals(new String[] { "a", ",c" }, Strings.splitPreserveAllTokens("a,,c", ",", 2));
    }

    @Test
    @DisplayName("Test splitToLines()")
    public void testSplitToLines() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines("a\nb\nc"));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\r\nb"));
        assertArrayEquals(new String[] { "" }, Strings.splitToLines(""));
        assertArrayEquals(new String[] {}, Strings.splitToLines(null));
    }

    @Test
    @DisplayName("Test splitToLines() with trim and omitEmpty")
    public void testSplitToLines_WithOptions() {
        assertArrayEquals(new String[] { "a", "b", "c" }, Strings.splitToLines(" a \n b \n c ", true, false));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines("a\n\nb", false, true));
        assertArrayEquals(new String[] { "a", "b" }, Strings.splitToLines(" a \n\n b ", true, true));
    }

    @Test
    @DisplayName("Test trim()")
    public void testTrim() {
        assertEquals("abc", Strings.trim("  abc  "));
        assertEquals("abc", Strings.trim("abc"));
        assertEquals("", Strings.trim("   "));
        assertNull(Strings.trim((String) null));
    }

    @Test
    @DisplayName("Test trim() with array")
    public void testTrim_Array() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.trim(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    @DisplayName("Test trimToNull()")
    public void testTrimToNull() {
        assertEquals("abc", Strings.trimToNull("  abc  "));
        assertNull(Strings.trimToNull("   "));
        assertNull(Strings.trimToNull((String) null));
    }

    @Test
    @DisplayName("Test trimToNull() with array")
    public void testTrimToNull_Array() {
        String[] array = { "  a  ", "   ", "c" };
        Strings.trimToNull(array);
        assertArrayEquals(new String[] { "a", null, "c" }, array);
    }

    @Test
    @DisplayName("Test trimToEmpty()")
    public void testTrimToEmpty() {
        assertEquals("abc", Strings.trimToEmpty("  abc  "));
        assertEquals("", Strings.trimToEmpty("   "));
        assertEquals("", Strings.trimToEmpty((String) null));
    }

    @Test
    @DisplayName("Test trimToEmpty() with array")
    public void testTrimToEmpty_Array() {
        String[] array = { "  a  ", "   ", null };
        Strings.trimToEmpty(array);
        assertArrayEquals(new String[] { "a", "", "" }, array);
    }

    @Test
    @DisplayName("Test strip()")
    public void testStrip() {
        assertEquals("abc", Strings.strip("  abc  "));
        assertEquals("abc", Strings.strip("abc"));
        assertEquals("", Strings.strip("   "));
        assertNull(Strings.strip((String) null));
    }

    @Test
    @DisplayName("Test strip() with array")
    public void testStrip_Array() {
        String[] array = { "  a  ", " b ", "c" };
        Strings.strip(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    @DisplayName("Test stripToNull()")
    public void testStripToNull() {
        assertEquals("abc", Strings.stripToNull("  abc  "));
        assertNull(Strings.stripToNull("   "));
        assertNull(Strings.stripToNull((String) null));
    }

    @Test
    @DisplayName("Test stripToEmpty()")
    public void testStripToEmpty() {
        assertEquals("abc", Strings.stripToEmpty("  abc  "));
        assertEquals("", Strings.stripToEmpty("   "));
        assertEquals("", Strings.stripToEmpty((String) null));
    }

    @Test
    @DisplayName("Test strip() with custom chars")
    public void testStrip_WithChars() {
        assertEquals("abc", Strings.strip("xxabcxx", "x"));
        assertEquals("abc", Strings.strip("--abc--", "-"));
        assertNull(Strings.strip((String) null, "x"));
    }

    @Test
    @DisplayName("Test stripStart()")
    public void testStripStart() {
        assertEquals("abc  ", Strings.stripStart("  abc  "));
        assertEquals("abc", Strings.stripStart("abc"));
        assertNull(Strings.stripStart((String) null));
    }

    @Test
    @DisplayName("Test stripStart() with custom chars")
    public void testStripStart_WithChars() {
        assertEquals("abcxx", Strings.stripStart("xxabcxx", "x"));
        assertNull(Strings.stripStart((String) null, "x"));
    }

    @Test
    @DisplayName("Test stripEnd()")
    public void testStripEnd() {
        assertEquals("  abc", Strings.stripEnd("  abc  "));
        assertEquals("abc", Strings.stripEnd("abc"));
        assertNull(Strings.stripEnd((String) null));
    }

    @Test
    @DisplayName("Test stripEnd() with custom chars")
    public void testStripEnd_WithChars() {
        assertEquals("xxabc", Strings.stripEnd("xxabcxx", "x"));
        assertNull(Strings.stripEnd((String) null, "x"));
    }

    @Test
    @DisplayName("Test stripAccents()")
    public void testStripAccents() {
        assertEquals("aeiou", Strings.stripAccents("\u00e0\u00e9\u00ed\u00f3\u00fa"));
        assertEquals("abc", Strings.stripAccents("abc"));
        assertNull(Strings.stripAccents((String) null));
    }

    @Test
    @DisplayName("Test stripAccents() with array")
    public void testStripAccents_Array() {
        String[] array = { "\u00e0bc", "xyz" };
        Strings.stripAccents(array);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    @DisplayName("Test chomp()")
    public void testChomp() {
        assertEquals("abc", Strings.chomp("abc\n"));
        assertEquals("abc", Strings.chomp("abc\r\n"));
        assertEquals("abc", Strings.chomp("abc\r"));
        assertEquals("abc", Strings.chomp("abc"));
        assertNull(Strings.chomp((String) null));
    }

    @Test
    @DisplayName("Test chomp() with array")
    public void testChomp_Array() {
        String[] array = { "a\n", "b\r\n", "c" };
        Strings.chomp(array);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    @DisplayName("Test chop()")
    public void testChop() {
        assertEquals("ab", Strings.chop("abc"));
        assertEquals("abc", Strings.chop("abc\r\n"));
        assertEquals("", Strings.chop("a"));
        assertEquals("", Strings.chop(""));
        assertNull(Strings.chop((String) null));
    }

    @Test
    @DisplayName("Test chop() with array")
    public void testChop_Array() {
        String[] array = { "abc", "xy", "z" };
        Strings.chop(array);
        assertArrayEquals(new String[] { "ab", "x", "" }, array);
    }

    @Test
    @DisplayName("Test truncate()")
    public void testTruncate() {
        assertEquals("abc", Strings.truncate("abcdef", 3));
        assertEquals("abc", Strings.truncate("abc", 5));
        assertNull(Strings.truncate((String) null, 3));
    }

    @Test
    @DisplayName("Test truncate() with offset")
    public void testTruncate_WithOffset() {
        assertEquals("cde", Strings.truncate("abcdef", 2, 3));
        assertEquals("abc", Strings.truncate("abc", 0, 5));
    }

    @Test
    @DisplayName("Test truncate() with array")
    public void testTruncate_Array() {
        String[] array = { "abcdef", "xyz" };
        Strings.truncate(array, 3);
        assertArrayEquals(new String[] { "abc", "xyz" }, array);
    }

    @Test
    @DisplayName("Test truncate() with array and offset")
    public void testTruncate_ArrayWithOffset() {
        String[] array = { "abcdef", "xyz" };
        Strings.truncate(array, 1, 3);
        assertArrayEquals(new String[] { "bcd", "yz" }, array);
    }

    @Test
    @DisplayName("Test deleteWhitespace()")
    public void testDeleteWhitespace() {
        assertEquals("abc", Strings.deleteWhitespace("a b c"));
        assertEquals("abc", Strings.deleteWhitespace("  a  b  c  "));
        assertEquals("abc", Strings.deleteWhitespace("abc"));
        assertNull(Strings.deleteWhitespace((String) null));
    }

    @Test
    @DisplayName("Test deleteWhitespace() with array")
    public void testDeleteWhitespace_Array() {
        String[] array = { "a b", " x y z " };
        Strings.deleteWhitespace(array);
        assertArrayEquals(new String[] { "ab", "xyz" }, array);
    }

    @Test
    @DisplayName("Test appendIfMissing()")
    public void testAppendIfMissing() {
        assertEquals("abc.txt", Strings.appendIfMissing("abc", ".txt"));
        assertEquals("abc.txt", Strings.appendIfMissing("abc.txt", ".txt"));
        assertEquals(".txt", Strings.appendIfMissing(null, ".txt"));
    }

    @Test
    @DisplayName("Test appendIfMissing() throws on empty suffix")
    public void testAppendIfMissing_EmptySuffix() {
        assertThrows(IllegalArgumentException.class, () -> Strings.appendIfMissing("abc", ""));
    }

    @Test
    @DisplayName("Test appendIfMissingIgnoreCase()")
    public void testAppendIfMissingIgnoreCase() {
        assertEquals("abc.txt", Strings.appendIfMissingIgnoreCase("abc", ".txt"));
        assertEquals("abc.TXT", Strings.appendIfMissingIgnoreCase("abc.TXT", ".txt"));
        assertEquals(".txt", Strings.appendIfMissingIgnoreCase(null, ".txt"));
    }

    @Test
    @DisplayName("Test prependIfMissing()")
    public void testPrependIfMissing() {
        assertEquals("http://abc", Strings.prependIfMissing("abc", "http://"));
        assertEquals("http://abc", Strings.prependIfMissing("http://abc", "http://"));
        assertEquals("http://", Strings.prependIfMissing(null, "http://"));
    }

    @Test
    @DisplayName("Test prependIfMissingIgnoreCase()")
    public void testPrependIfMissingIgnoreCase() {
        assertEquals("http://abc", Strings.prependIfMissingIgnoreCase("abc", "http://"));
        assertEquals("HTTP://abc", Strings.prependIfMissingIgnoreCase("HTTP://abc", "http://"));
        assertEquals("http://", Strings.prependIfMissingIgnoreCase(null, "http://"));
    }

    @Test
    @DisplayName("Test wrapIfMissing() with same prefix/suffix")
    public void testWrapIfMissing() {
        assertEquals("'abc'", Strings.wrapIfMissing("abc", "'"));
        assertEquals("'abc'", Strings.wrapIfMissing("'abc'", "'"));
        assertEquals("''", Strings.wrapIfMissing(null, "'"));
    }

    @Test
    @DisplayName("Test wrapIfMissing() with different prefix and suffix")
    public void testWrapIfMissing_DifferentPrefixSuffix() {
        assertEquals("<abc>", Strings.wrapIfMissing("abc", "<", ">"));
        assertEquals("<abc>", Strings.wrapIfMissing("<abc>", "<", ">"));
        assertEquals("<>", Strings.wrapIfMissing(null, "<", ">"));
    }

    @Test
    @DisplayName("Test wrap()")
    public void testWrap() {
        assertEquals("'abc'", Strings.wrap("abc", "'"));
        assertEquals("''abc''", Strings.wrap("'abc'", "'"));
        assertEquals("''", Strings.wrap(null, "'"));
    }

    @Test
    @DisplayName("Test wrap() with different prefix and suffix")
    public void testWrap_DifferentPrefixSuffix() {
        assertEquals("<abc>", Strings.wrap("abc", "<", ">"));
        assertEquals("<<abc>>", Strings.wrap("<abc>", "<", ">"));
        assertEquals("<>", Strings.wrap(null, "<", ">"));
    }

    @Test
    @DisplayName("Test unwrap()")
    public void testUnwrap() {
        assertEquals("abc", Strings.unwrap("'abc'", "'"));
        assertEquals("abc", Strings.unwrap("abc", "'"));
        assertNull(Strings.unwrap(null, "'"));
    }

    @Test
    @DisplayName("Test unwrap() with different prefix and suffix")
    public void testUnwrap_DifferentPrefixSuffix() {
        assertEquals("abc", Strings.unwrap("<abc>", "<", ">"));
        assertEquals("abc", Strings.unwrap("abc", "<", ">"));
        assertNull(Strings.unwrap(null, "<", ">"));
    }

    @Test
    @DisplayName("Test isLowerCase()")
    public void testIsLowerCase() {
        assertTrue(Strings.isLowerCase('a'));
        assertTrue(Strings.isLowerCase('z'));
        assertFalse(Strings.isLowerCase('A'));
        assertFalse(Strings.isLowerCase('1'));
    }

    @Test
    @DisplayName("Test isAsciiLowerCase()")
    public void testIsAsciiLowerCase() {
        assertTrue(Strings.isAsciiLowerCase('a'));
        assertTrue(Strings.isAsciiLowerCase('z'));
        assertFalse(Strings.isAsciiLowerCase('A'));
        assertFalse(Strings.isAsciiLowerCase('1'));
    }

    @Test
    @DisplayName("Test isUpperCase()")
    public void testIsUpperCase() {
        assertTrue(Strings.isUpperCase('A'));
        assertTrue(Strings.isUpperCase('Z'));
        assertFalse(Strings.isUpperCase('a'));
        assertFalse(Strings.isUpperCase('1'));
    }

    @Test
    @DisplayName("Test isAsciiUpperCase()")
    public void testIsAsciiUpperCase() {
        assertTrue(Strings.isAsciiUpperCase('A'));
        assertTrue(Strings.isAsciiUpperCase('Z'));
        assertFalse(Strings.isAsciiUpperCase('a'));
        assertFalse(Strings.isAsciiUpperCase('1'));
    }

    @Test
    @DisplayName("Test isAllLowerCase()")
    public void testIsAllLowerCase() {
        assertTrue(Strings.isAllLowerCase("abc"));
        assertTrue(Strings.isAllLowerCase("a"));
        assertFalse(Strings.isAllLowerCase("Abc"));
        assertFalse(Strings.isAllLowerCase("ABC"));
        assertTrue(Strings.isAllLowerCase(null));
        assertTrue(Strings.isAllLowerCase(""));
    }

    @Test
    @DisplayName("Test isAllUpperCase()")
    public void testIsAllUpperCase() {
        assertTrue(Strings.isAllUpperCase("ABC"));
        assertTrue(Strings.isAllUpperCase("A"));
        assertFalse(Strings.isAllUpperCase("Abc"));
        assertFalse(Strings.isAllUpperCase("abc"));
        assertTrue(Strings.isAllUpperCase(null));
        assertTrue(Strings.isAllUpperCase(""));
    }

    @Test
    @DisplayName("Test isMixedCase()")
    public void testIsMixedCase() {
        assertTrue(Strings.isMixedCase("Abc"));
        assertTrue(Strings.isMixedCase("aBc"));
        assertFalse(Strings.isMixedCase("abc"));
        assertFalse(Strings.isMixedCase("ABC"));
        assertFalse(Strings.isMixedCase(null));
        assertFalse(Strings.isMixedCase(""));
    }

    @Test
    @DisplayName("Test isDigit()")
    public void testIsDigit() {
        assertTrue(Strings.isDigit('0'));
        assertTrue(Strings.isDigit('9'));
        assertFalse(Strings.isDigit('a'));
        assertFalse(Strings.isDigit('A'));
    }

    @Test
    @DisplayName("Test isLetter()")
    public void testIsLetter() {
        assertTrue(Strings.isLetter('a'));
        assertTrue(Strings.isLetter('A'));
        assertTrue(Strings.isLetter('z'));
        assertFalse(Strings.isLetter('1'));
        assertFalse(Strings.isLetter('!'));
    }

    @Test
    @DisplayName("Test isLetterOrDigit()")
    public void testIsLetterOrDigit() {
        assertTrue(Strings.isLetterOrDigit('a'));
        assertTrue(Strings.isLetterOrDigit('A'));
        assertTrue(Strings.isLetterOrDigit('1'));
        assertFalse(Strings.isLetterOrDigit('!'));
        assertFalse(Strings.isLetterOrDigit(' '));
    }

    @Test
    @DisplayName("Test isAscii()")
    public void testIsAscii() {
        assertTrue(Strings.isAscii('a'));
        assertTrue(Strings.isAscii('A'));
        assertTrue(Strings.isAscii('1'));
        assertTrue(Strings.isAscii(' '));
        assertFalse(Strings.isAscii('\u00e9'));
    }

    @Test
    @DisplayName("Test isAsciiPrintable() with char")
    public void testIsAsciiPrintable_Char() {
        assertTrue(Strings.isAsciiPrintable('a'));
        assertTrue(Strings.isAsciiPrintable('1'));
        assertTrue(Strings.isAsciiPrintable(' '));
        assertFalse(Strings.isAsciiPrintable('\t'));
        assertFalse(Strings.isAsciiPrintable('\n'));
    }

    @Test
    @DisplayName("Test isAsciiControl()")
    public void testIsAsciiControl() {
        assertTrue(Strings.isAsciiControl('\t'));
        assertTrue(Strings.isAsciiControl('\n'));
        assertFalse(Strings.isAsciiControl('a'));
        assertFalse(Strings.isAsciiControl(' '));
    }

    @Test
    @DisplayName("Test isAsciiAlpha() with char")
    public void testIsAsciiAlpha_Char() {
        assertTrue(Strings.isAsciiAlpha('a'));
        assertTrue(Strings.isAsciiAlpha('Z'));
        assertFalse(Strings.isAsciiAlpha('1'));
        assertFalse(Strings.isAsciiAlpha(' '));
    }

    @Test
    @DisplayName("Test isAsciiAlphaUpper()")
    public void testIsAsciiAlphaUpper() {
        assertTrue(Strings.isAsciiAlphaUpper('A'));
        assertTrue(Strings.isAsciiAlphaUpper('Z'));
        assertFalse(Strings.isAsciiAlphaUpper('a'));
        assertFalse(Strings.isAsciiAlphaUpper('1'));
    }

    @Test
    @DisplayName("Test isAsciiAlphaLower()")
    public void testIsAsciiAlphaLower() {
        assertTrue(Strings.isAsciiAlphaLower('a'));
        assertTrue(Strings.isAsciiAlphaLower('z'));
        assertFalse(Strings.isAsciiAlphaLower('A'));
        assertFalse(Strings.isAsciiAlphaLower('1'));
    }

    @Test
    @DisplayName("Test isAsciiNumeric() with char")
    public void testIsAsciiNumeric_Char() {
        assertTrue(Strings.isAsciiNumeric('0'));
        assertTrue(Strings.isAsciiNumeric('9'));
        assertFalse(Strings.isAsciiNumeric('a'));
        assertFalse(Strings.isAsciiNumeric(' '));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumeric() with char")
    public void testIsAsciiAlphanumeric_Char() {
        assertTrue(Strings.isAsciiAlphanumeric('a'));
        assertTrue(Strings.isAsciiAlphanumeric('A'));
        assertTrue(Strings.isAsciiAlphanumeric('1'));
        assertFalse(Strings.isAsciiAlphanumeric(' '));
        assertFalse(Strings.isAsciiAlphanumeric('!'));
    }

    @Test
    @DisplayName("Test isAsciiPrintable() with string")
    public void testIsAsciiPrintable_String() {
        assertFalse(Strings.isAsciiPrintable(null));
        assertFalse(Strings.isAsciiPrintable("abc\t123"));
        assertTrue(Strings.isAsciiPrintable("abc123"));
        assertTrue(Strings.isAsciiPrintable("abc 123"));
        assertTrue(Strings.isAsciiPrintable(""));
    }

    @Test
    @DisplayName("Test isAsciiAlpha() with string")
    public void testIsAsciiAlpha_String() {
        assertTrue(Strings.isAsciiAlpha("abc"));
        assertTrue(Strings.isAsciiAlpha("ABC"));
        assertFalse(Strings.isAsciiAlpha("abc123"));
        assertFalse(Strings.isAsciiAlpha(null));
        assertFalse(Strings.isAsciiAlpha(""));
    }

    @Test
    @DisplayName("Test isAsciiAlphaSpace()")
    public void testIsAsciiAlphaSpace() {
        assertTrue(Strings.isAsciiAlphaSpace(""));
        assertTrue(Strings.isAsciiAlphaSpace("abc"));
        assertTrue(Strings.isAsciiAlphaSpace("abc def"));
        assertFalse(Strings.isAsciiAlphaSpace("abc123"));
        assertFalse(Strings.isAsciiAlphaSpace(null));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumeric() with string")
    public void testIsAsciiAlphanumeric_String() {
        assertTrue(Strings.isAsciiAlphanumeric("abc123"));
        assertTrue(Strings.isAsciiAlphanumeric("ABC"));
        assertFalse(Strings.isAsciiAlphanumeric("abc 123"));
        assertFalse(Strings.isAsciiAlphanumeric(null));
        assertFalse(Strings.isAsciiAlphanumeric(""));
    }

    @Test
    @DisplayName("Test isAsciiAlphanumericSpace()")
    public void testIsAsciiAlphanumericSpace() {
        assertTrue(Strings.isAsciiAlphanumericSpace("abc123"));
        assertTrue(Strings.isAsciiAlphanumericSpace("abc 123"));
        assertFalse(Strings.isAsciiAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAsciiAlphanumericSpace(null));
        assertTrue(Strings.isAsciiAlphanumericSpace(""));
    }

    @Test
    @DisplayName("Test isAsciiNumeric() with string")
    public void testIsAsciiNumeric_String() {
        assertTrue(Strings.isAsciiNumeric("123"));
        assertTrue(Strings.isAsciiNumeric("0"));
        assertFalse(Strings.isAsciiNumeric("abc"));
        assertFalse(Strings.isAsciiNumeric("12.3"));
        assertFalse(Strings.isAsciiNumeric(null));
        assertFalse(Strings.isAsciiNumeric(""));
    }

    @Test
    @DisplayName("Test isAlpha()")
    public void testIsAlpha() {
        assertTrue(Strings.isAlpha("abc"));
        assertTrue(Strings.isAlpha("ABC"));
        assertFalse(Strings.isAlpha("abc123"));
        assertFalse(Strings.isAlpha("abc "));
        assertFalse(Strings.isAlpha(null));
        assertFalse(Strings.isAlpha(""));
    }

    @Test
    @DisplayName("Test isAlphaSpace()")
    public void testIsAlphaSpace() {
        assertTrue(Strings.isAlphaSpace("abc"));
        assertTrue(Strings.isAlphaSpace("abc def"));
        assertFalse(Strings.isAlphaSpace("abc123"));
        assertFalse(Strings.isAlphaSpace(null));
        assertTrue(Strings.isAlphaSpace(""));
    }

    @Test
    @DisplayName("Test isAlphanumeric()")
    public void testIsAlphanumeric() {
        assertTrue(Strings.isAlphanumeric("abc123"));
        assertTrue(Strings.isAlphanumeric("ABC"));
        assertFalse(Strings.isAlphanumeric("abc 123"));
        assertFalse(Strings.isAlphanumeric(null));
        assertFalse(Strings.isAlphanumeric(""));
    }

    @Test
    @DisplayName("Test isAlphanumericSpace()")
    public void testIsAlphanumericSpace() {
        assertTrue(Strings.isAlphanumericSpace(""));
        assertTrue(Strings.isAlphanumericSpace("abc123"));
        assertTrue(Strings.isAlphanumericSpace("abc 123"));
        assertTrue(Strings.isAlphanumericSpace("你好 123"));
        assertTrue(Strings.isAlphanumericSpace("café au lait 2023"));
        assertFalse(Strings.isAlphanumericSpace("abc!123"));
        assertFalse(Strings.isAlphanumericSpace(null));
    }

    @Test
    @DisplayName("Test isNumeric()")
    public void testIsNumeric() {
        assertTrue(Strings.isNumeric("123"));
        assertTrue(Strings.isNumeric("0"));
        assertFalse(Strings.isNumeric("12.3"));
        assertFalse(Strings.isNumeric("abc"));
        assertFalse(Strings.isNumeric(null));
        assertFalse(Strings.isNumeric(""));
    }

    @Test
    @DisplayName("Test isNumericSpace()")
    public void testIsNumericSpace() {
        assertTrue(Strings.isNumericSpace(""));
        assertTrue(Strings.isNumericSpace("123"));
        assertTrue(Strings.isNumericSpace("1 2 3"));
        assertFalse(Strings.isNumericSpace("12.3"));
        assertFalse(Strings.isNumericSpace(null));
    }

    @Test
    @DisplayName("Test isWhitespace()")
    public void testIsWhitespace() {
        assertTrue(Strings.isWhitespace(""));
        assertTrue(Strings.isWhitespace("   "));
        assertTrue(Strings.isWhitespace("\t\n\r"));
        assertTrue(Strings.isWhitespace(" "));
        assertFalse(Strings.isWhitespace("abc"));
        assertFalse(Strings.isWhitespace(" a "));
        assertFalse(Strings.isWhitespace(null));
    }

    @Test
    @DisplayName("Test isNumber()")
    public void testIsNumber() {
        assertTrue(Strings.isNumber("123"));
        assertTrue(Strings.isNumber("12.3"));
        assertTrue(Strings.isNumber("-123"));
        assertTrue(Strings.isNumber("1.23e4"));
        assertFalse(Strings.isNumber("abc"));
        assertFalse(Strings.isNumber(null));
        assertFalse(Strings.isNumber(""));
    }

    @Test
    @DisplayName("Test isAsciiDigitalNumber()")
    public void testIsAsciiDigitalNumber() {
        assertTrue(Strings.isAsciiDigitalNumber("123"));
        assertTrue(Strings.isAsciiDigitalNumber("-123"));
        assertTrue(Strings.isAsciiDigitalNumber("12.3"));
        assertFalse(Strings.isAsciiDigitalNumber("abc"));
        assertFalse(Strings.isAsciiDigitalNumber(null));
        assertFalse(Strings.isAsciiDigitalNumber(""));
    }

    @Test
    @DisplayName("Test convertWords() with function")
    public void testConvertWords() {
        assertEquals("HELLO WORLD", Strings.convertWords("hello world", String::toUpperCase));
        assertEquals("abc def", Strings.convertWords("ABC DEF", String::toLowerCase));
        assertNull(Strings.convertWords(null, String::toUpperCase));
    }

    @Test
    @DisplayName("Test convertWords() with delimiter")
    public void testConvertWords_WithDelimiter() {
        assertEquals("HELLO-WORLD", Strings.convertWords("hello-world", "-", String::toUpperCase));
        assertNull(Strings.convertWords(null, "-", String::toUpperCase));
    }

    @Test
    @DisplayName("Test convertWords() with excluded words")
    public void testConvertWords_WithExcludedWords() {
        List<String> excluded = Arrays.asList("the", "and");
        assertEquals("HELLO the WORLD", Strings.convertWords("hello the world", " ", excluded, String::toUpperCase));
    }

    @Test
    @DisplayName("Test base64Encode() with byte array")
    public void testBase64Encode() {
        byte[] data = "Hello World".getBytes();
        String encoded = Strings.base64Encode(data);
        assertNotNull(encoded);
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64Encode((byte[]) null));
    }

    @Test
    @DisplayName("Test base64EncodeString()")
    public void testBase64EncodeString() {
        String encoded = Strings.base64EncodeString("Hello World");
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64EncodeString(null));
    }

    @Test
    @DisplayName("Test base64EncodeUtf8String()")
    public void testBase64EncodeUtf8String() {
        String encoded = Strings.base64EncodeUtf8String("Hello World");
        assertEquals("SGVsbG8gV29ybGQ=", encoded);
        assertEquals("", Strings.base64EncodeUtf8String(null));
    }

    @Test
    @DisplayName("Test base64Decode()")
    public void testBase64Decode() {
        byte[] decoded = Strings.base64Decode("SGVsbG8gV29ybGQ=");
        assertArrayEquals("Hello World".getBytes(), decoded);
        assertArrayEquals("".getBytes(), Strings.base64Decode((String) null));
    }

    @Test
    @DisplayName("Test base64DecodeToString()")
    public void testBase64DecodeToString() {
        String decoded = Strings.base64DecodeToString("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64DecodeToString(null));
    }

    @Test
    @DisplayName("Test base64DecodeToUtf8String()")
    public void testBase64DecodeToUtf8String() {
        String decoded = Strings.base64DecodeToUtf8String("SGVsbG8gV29ybGQ=");
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64DecodeToUtf8String(null));
    }

    @Test
    @DisplayName("Test base64UrlEncode()")
    public void testBase64UrlEncode() {
        byte[] data = "Hello+World/Test".getBytes();
        String encoded = Strings.base64UrlEncode(data);
        assertNotNull(encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));
        assertEquals("", Strings.base64UrlEncode((byte[]) null));
    }

    @Test
    @DisplayName("Test base64UrlDecode()")
    public void testBase64UrlDecode() {
        String encoded = Strings.base64UrlEncode("Hello+World/Test".getBytes());
        byte[] decoded = Strings.base64UrlDecode(encoded);
        assertArrayEquals("Hello+World/Test".getBytes(), decoded);
        assertArrayEquals("".getBytes(), Strings.base64UrlDecode(""));
        assertArrayEquals("".getBytes(), Strings.base64UrlDecode((String) null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToString()")
    public void testBase64UrlDecodeToString() {
        String encoded = Strings.base64UrlEncode("Hello World".getBytes());
        String decoded = Strings.base64UrlDecodeToString(encoded);
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64UrlDecodeToString(null));
    }

    @Test
    @DisplayName("Test base64UrlDecodeToUtf8String()")
    public void testBase64UrlDecodeToUtf8String() {
        String encoded = Strings.base64UrlEncode("Hello World".getBytes());
        String decoded = Strings.base64UrlDecodeToUtf8String(encoded);
        assertEquals("Hello World", decoded);
        assertEquals("", Strings.base64UrlDecodeToUtf8String(null));
    }

    @Test
    @DisplayName("Test isBase64()")
    public void testIsBase64() {
        assertTrue(Strings.isBase64("SGVsbG8gV29ybGQ="));
        assertTrue(Strings.isBase64("YWJjZGVm"));
        assertFalse(Strings.isBase64("Hello World!"));
        assertFalse(Strings.isBase64("ABC!@#"));
        assertFalse(Strings.isBase64((String) null));
        assertFalse(Strings.isBase64((byte[]) null));
        assertTrue(Strings.isBase64(""));
    }

    @Test
    @DisplayName("Test urlEncode()")
    public void testUrlEncode() {
        String encoded = Strings.urlEncode("Hello World");
        assertEquals("Hello+World", encoded);
        String encoded2 = Strings.urlEncode("a=b&c=d");
        assertEquals("a=b&c=d", encoded2);
        assertEquals("", Strings.urlEncode(null));
    }

    @Test
    @DisplayName("Test urlDecode()")
    public void testUrlDecode() {
        Map<String, String> decoded = Strings.urlDecode(Strings.urlEncode("a=b&c=d"));
        assertEquals(Map.of("a", "b", "c", "d"), decoded);
        assertEquals(Map.of(), Strings.urlDecode(null));
    }

    @Test
    @DisplayName("Test indexOf() with char")
    public void testIndexOf_Char() {
        assertEquals(0, Strings.indexOf("abc", 'a'));
        assertEquals(2, Strings.indexOf("abc", 'c'));
        assertEquals(-1, Strings.indexOf("abc", 'x'));
        assertEquals(-1, Strings.indexOf(null, 'a'));
    }

    @Test
    @DisplayName("Test indexOf() with char and fromIndex")
    public void testIndexOf_CharWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", 'a', 1));
        assertEquals(-1, Strings.indexOf("abc", 'a', 5));
        assertEquals(-1, Strings.indexOf(null, 'a', 0));
    }

    @Test
    @DisplayName("Test indexOf() with string")
    public void testIndexOf_String() {
        assertEquals(0, Strings.indexOf("abcdef", "abc"));
        assertEquals(3, Strings.indexOf("abcdef", "def"));
        assertEquals(-1, Strings.indexOf("abcdef", "xyz"));
        assertEquals(-1, Strings.indexOf(null, "abc"));
    }

    @Test
    @DisplayName("Test indexOf() with string and fromIndex")
    public void testIndexOf_StringWithFromIndex() {
        assertEquals(3, Strings.indexOf("abcabc", "abc", 1));
        assertEquals(-1, Strings.indexOf("abc", "abc", 5));
        assertEquals(-1, Strings.indexOf(null, "abc", 0));
    }

    @Test
    @DisplayName("Test indexOfAny() with char array")
    public void testIndexOfAny_CharArray() {
        assertEquals(0, Strings.indexOfAny("abc", 'a', 'x'));
        assertEquals(1, Strings.indexOfAny("abc", 'b', 'c'));
        assertEquals(-1, Strings.indexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test indexOfAny() with char array and fromIndex")
    public void testIndexOfAny_CharArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, 'a'));
        assertEquals(-1, Strings.indexOfAny("abc", 5, 'a'));
    }

    @Test
    @DisplayName("Test indexOfAny() with string array")
    public void testIndexOfAny_StringArray() {
        assertEquals(0, Strings.indexOfAny("abcdef", "abc", "xyz"));
        assertEquals(3, Strings.indexOfAny("abcdef", "def", "xyz"));
        assertEquals(-1, Strings.indexOfAny("abcdef", "xyz", "123"));
        assertEquals(-1, Strings.indexOfAny(null, "abc", "def"));
    }

    @Test
    @DisplayName("Test indexOfAny() with string array and fromIndex")
    public void testIndexOfAny_StringArrayWithFromIndex() {
        assertEquals(3, Strings.indexOfAny("abcabc", 1, "abc"));
        assertEquals(-1, Strings.indexOfAny("abc", 5, "abc"));
    }

    @Test
    @DisplayName("Test indexOfAnyBut() with char array")
    public void testIndexOfAnyBut() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 'a'));
        assertEquals(0, Strings.indexOfAnyBut("abc", 'x', 'y'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 'a'));
        assertEquals(-1, Strings.indexOfAnyBut(null, 'a'));
    }

    @Test
    @DisplayName("Test indexOfAnyBut() with fromIndex")
    public void testIndexOfAnyBut_WithFromIndex() {
        assertEquals(3, Strings.indexOfAnyBut("aaabbb", 3, 'a'));
        assertEquals(-1, Strings.indexOfAnyBut("aaa", 0, 'a'));
    }

    @Test
    @DisplayName("Test indexOfIgnoreCase()")
    public void testIndexOfIgnoreCase() {
        assertEquals(0, Strings.indexOfIgnoreCase("AbCdEf", "abc"));
        assertEquals(3, Strings.indexOfIgnoreCase("abcDEF", "def"));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.indexOfIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test indexOfIgnoreCase() with fromIndex")
    public void testIndexOfIgnoreCase_WithFromIndex() {
        assertEquals(3, Strings.indexOfIgnoreCase("AbCaBc", "abc", 1));
        assertEquals(-1, Strings.indexOfIgnoreCase("abc", "abc", 5));
    }

    @Test
    @DisplayName("Test indexOfDifference()")
    public void testIndexOfDifference() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc"));
        assertEquals(0, Strings.indexOfDifference(null, "abc"));
        assertEquals(-1, Strings.indexOfDifference(null, null));
        assertEquals(-1, Strings.indexOfDifference("", ""));
        assertEquals(-1, Strings.indexOfDifference(null, ""));
    }

    @Test
    @DisplayName("Test indexOfDifference() with array")
    public void testIndexOfDifference_Array() {
        assertEquals(3, Strings.indexOfDifference("abcxyz", "abcdef", "abc123"));
        assertEquals(0, Strings.indexOfDifference("abc", "xyz"));
        assertEquals(-1, Strings.indexOfDifference("abc", "abc", "abc"));
    }

    @Test
    @DisplayName("Test indicesOf()")
    public void testIndicesOf() {
        int[] indices = Strings.indicesOf("abcabc", "abc").toArray();
        assertArrayEquals(new int[] { 0, 3 }, indices);
        int[] indices2 = Strings.indicesOf("abc", "xyz").toArray();
        assertArrayEquals(new int[0], indices2);
    }

    @Test
    @DisplayName("Test indicesOfIgnoreCase()")
    public void testIndicesOfIgnoreCase() {
        int[] indices = Strings.indicesOfIgnoreCase("AbCaBc", "abc").toArray();
        assertArrayEquals(new int[] { 0, 3 }, indices);
        int[] indices2 = Strings.indicesOfIgnoreCase("abc", "xyz").toArray();
        assertArrayEquals(new int[0], indices2);
    }

    @Test
    @DisplayName("Test lastIndexOf() with char")
    public void testLastIndexOf_Char() {
        assertEquals(3, Strings.lastIndexOf("abcabc", 'a'));
        assertEquals(5, Strings.lastIndexOf("abcabc", 'c'));
        assertEquals(-1, Strings.lastIndexOf("abc", 'x'));
        assertEquals(-1, Strings.lastIndexOf(null, 'a'));
    }

    @Test
    @DisplayName("Test lastIndexOf() with char and startIndexFromBack")
    public void testLastIndexOf_CharWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", 'a', 2));
        assertEquals(-1, Strings.lastIndexOf("abc", 'c', 1));
    }

    @Test
    @DisplayName("Test lastIndexOf() with string")
    public void testLastIndexOf_String() {
        assertEquals(3, Strings.lastIndexOf("abcabc", "abc"));
        assertEquals(0, Strings.lastIndexOf("abc", "abc"));
        assertEquals(-1, Strings.lastIndexOf("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOf(null, "abc"));
    }

    @Test
    @DisplayName("Test lastIndexOf() with string and startIndexFromBack")
    public void testLastIndexOf_StringWithStartIndex() {
        assertEquals(0, Strings.lastIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOf("abc", "abc", -1));
    }

    @Test
    @DisplayName("Test lastIndexOfIgnoreCase()")
    public void testLastIndexOfIgnoreCase() {
        assertEquals(3, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc"));
        assertEquals(0, Strings.lastIndexOfIgnoreCase("ABC", "abc"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test lastIndexOfIgnoreCase() with startIndexFromBack")
    public void testLastIndexOfIgnoreCase_WithStartIndex() {
        assertEquals(0, Strings.lastIndexOfIgnoreCase("AbCaBc", "abc", 2));
        assertEquals(-1, Strings.lastIndexOfIgnoreCase("abc", "xyz", 5));
    }

    @Test
    @DisplayName("Test lastIndexOfAny() with char array")
    public void testLastIndexOfAny_CharArray() {
        assertEquals(0, Strings.lastIndexOfAny("abc", 'a', 'c'));
        assertEquals(5, Strings.lastIndexOfAny("abcabc", 'c'));
        assertEquals(-1, Strings.lastIndexOfAny("abc", 'x', 'y'));
        assertEquals(-1, Strings.lastIndexOfAny(null, 'a'));
    }

    @Test
    @DisplayName("Test lastIndexOfAny() with string array")
    public void testLastIndexOfAny_StringArray() {
        assertEquals(3, Strings.lastIndexOfAny("abcabc", "abc", "xyz"));
        assertEquals(-1, Strings.lastIndexOfAny("abc", "xyz", "123"));
        assertEquals(-1, Strings.lastIndexOfAny(null, "abc"));
    }

    @Test
    @DisplayName("Test ordinalIndexOf()")
    public void testOrdinalIndexOf() {
        assertEquals(0, Strings.ordinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.ordinalIndexOf("aabaabaa", "a", 3));
        assertEquals(5, Strings.ordinalIndexOf("aabaabaa", "b", 2));
        assertEquals(4, Strings.ordinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(0, Strings.ordinalIndexOf("abcabc", "abc", 1));
        assertEquals(3, Strings.ordinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.ordinalIndexOf("abcabc", "abc", 3));
        assertEquals(-1, Strings.ordinalIndexOf(null, "abc", 1));
        assertEquals(0, Strings.ordinalIndexOf("", "", 1));
        assertEquals(-1, Strings.ordinalIndexOf("", null, 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.ordinalIndexOf(null, null, 1));
    }

    @Test
    @DisplayName("Test lastOrdinalIndexOf()")
    public void testLastOrdinalIndexOf() {
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(4, Strings.lastOrdinalIndexOf("aabaabaa", "a", 3));
        assertEquals(0, Strings.lastOrdinalIndexOf("aabaabaa", "a", 6));
        assertEquals(2, Strings.lastOrdinalIndexOf("aabaabaa", "b", 2));
        assertEquals(1, Strings.lastOrdinalIndexOf("aabaabaa", "ab", 2));
        assertEquals(-1, Strings.lastOrdinalIndexOf("aabaabaa", "c", 1));
        assertEquals(7, Strings.lastOrdinalIndexOf("aabaabaa", "a", 1));
        assertEquals(3, Strings.lastOrdinalIndexOf("abcabc", "abc", 1));
        assertEquals(0, Strings.lastOrdinalIndexOf("abcabc", "abc", 2));
        assertEquals(-1, Strings.lastOrdinalIndexOf("abcabc", "abc", 3));
        assertEquals(0, Strings.lastOrdinalIndexOf("", "", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf("", null, 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, "", 1));
        assertEquals(-1, Strings.lastOrdinalIndexOf(null, null, 1));
    }

    @Test
    @DisplayName("Test smallestIndexOfAll()")
    public void testSmallestIndexOfAll() {
        assertEquals(0, Strings.smallestIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.smallestIndexOfAll("abcdefg", "def", "efg"));
        assertEquals(-1, Strings.smallestIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test smallestIndexOfAll() with fromIndex")
    public void testSmallestIndexOfAll_WithFromIndex() {
        assertEquals(3, Strings.smallestIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.smallestIndexOfAll("abc", 5, "abc"));
    }

    @Test
    @DisplayName("Test largestIndexOfAll()")
    public void testLargestIndexOfAll() {
        assertEquals(4, Strings.largestIndexOfAll("abcdefg", "abc", "efg"));
        assertEquals(3, Strings.largestIndexOfAll("abcdefg", "def", "abc"));
        assertEquals(-1, Strings.largestIndexOfAll("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test largestIndexOfAll() with fromIndex")
    public void testLargestIndexOfAll_WithFromIndex() {
        assertEquals(4, Strings.largestIndexOfAll("abcdefg", 2, "def", "efg"));
        assertEquals(-1, Strings.largestIndexOfAll("abc", 5, "abc"));
    }

    @Test
    @DisplayName("Test largestLastIndexOfAll()")
    public void testLargestLastIndexOfAll() {
        assertEquals(6, Strings.largestLastIndexOfAll("abcabcabc", "abc"));
        assertEquals(6, Strings.largestLastIndexOfAll("abcdefabc", "abc", "def"));
    }

    @Test
    @DisplayName("Test smallestLastIndexOfAll()")
    public void testSmallestLastIndexOfAll() {
        assertEquals(3, Strings.smallestLastIndexOfAll("abcdefabc", "abc", "def"));
        assertEquals(6, Strings.smallestLastIndexOfAll("abcabcabc", "abc"));
    }

    @Test
    @DisplayName("Test contains() with char")
    public void testContains_Char() {
        assertTrue(Strings.contains("abc", 'a'));
        assertTrue(Strings.contains("abc", 'c'));
        assertFalse(Strings.contains("abc", 'x'));
        assertFalse(Strings.contains(null, 'a'));
    }

    @Test
    @DisplayName("Test contains() with string")
    public void testContains_String() {
        assertTrue(Strings.contains("abcdef", "abc"));
        assertTrue(Strings.contains("abcdef", "def"));
        assertFalse(Strings.contains("abcdef", "xyz"));
        assertFalse(Strings.contains(null, "abc"));
    }

    @Test
    @DisplayName("Test containsIgnoreCase()")
    public void testContainsIgnoreCase() {
        assertTrue(Strings.containsIgnoreCase("AbCdEf", "abc"));
        assertTrue(Strings.containsIgnoreCase("abcdef", "DEF"));
        assertFalse(Strings.containsIgnoreCase("abc", "xyz"));
        assertFalse(Strings.containsIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsAll() with char array")
    public void testContainsAll_CharArray() {
        assertTrue(Strings.containsAll("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsAll("abc", 'a', 'x'));
        assertFalse(Strings.containsAll(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test containsAll() with string array")
    public void testContainsAll_StringArray() {
        assertTrue(Strings.containsAll("abcdefg", "abc", "def"));
        assertFalse(Strings.containsAll("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAll(null, "abc", "def"));
    }

    @Test
    @DisplayName("Test containsAllIgnoreCase()")
    public void testContainsAllIgnoreCase() {
        assertTrue(Strings.containsAllIgnoreCase("AbCdEfG", "abc", "def"));
        assertFalse(Strings.containsAllIgnoreCase("abcdef", "abc", "xyz"));
        assertFalse(Strings.containsAllIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsAny() with char array")
    public void testContainsAny_CharArray() {
        assertTrue(Strings.containsAny("abc", 'a', 'x'));
        assertTrue(Strings.containsAny("abc", 'c', 'y'));
        assertFalse(Strings.containsAny("abc", 'x', 'y'));
        assertFalse(Strings.containsAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test containsAny() with string array")
    public void testContainsAny_StringArray() {
        assertTrue(Strings.containsAny("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsAny("abcdef", "xyz", "def"));
        assertFalse(Strings.containsAny("abc", "xyz", "123"));
        assertFalse(Strings.containsAny(null, "abc", "def"));
    }

    @Test
    @DisplayName("Test containsAnyIgnoreCase()")
    public void testContainsAnyIgnoreCase() {
        assertTrue(Strings.containsAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertFalse(Strings.containsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.containsAnyIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsNone() with char array")
    public void testContainsNone_CharArray() {
        assertTrue(Strings.containsNone("abc", 'x', 'y'));
        assertTrue(Strings.containsNone("", 'a'));
        assertTrue(Strings.containsNone(null, 'a'));
        assertFalse(Strings.containsNone("abc", 'a', 'x'));
    }

    @Test
    @DisplayName("Test containsNone() with string array")
    public void testContainsNone_StringArray() {
        assertTrue(Strings.containsNone("abc", "xyz", "123"));
        assertFalse(Strings.containsNone("abcdef", "abc", "xyz"));
        assertTrue(Strings.containsNone(null, "abc"));
    }

    @Test
    @DisplayName("Test containsNoneIgnoreCase()")
    public void testContainsNoneIgnoreCase() {
        assertTrue(Strings.containsNoneIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.containsNoneIgnoreCase("AbCdEf", "abc", "xyz"));
        assertTrue(Strings.containsNoneIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test containsOnly() with char array")
    public void testContainsOnly() {
        assertTrue(Strings.containsOnly("aaa", 'a'));
        assertTrue(Strings.containsOnly("abc", 'a', 'b', 'c'));
        assertFalse(Strings.containsOnly("abc", 'a', 'b'));
        assertTrue(Strings.containsOnly(null, 'a'));
    }

    @Test
    @DisplayName("Test containsWhitespace()")
    public void testContainsWhitespace() {
        assertTrue(Strings.containsWhitespace("a b"));
        assertTrue(Strings.containsWhitespace("a\tb"));
        assertFalse(Strings.containsWhitespace("abc"));
        assertFalse(Strings.containsWhitespace(null));
    }

    @Test
    @DisplayName("Test countMatches() with char")
    public void testCountMatches_Char() {
        assertEquals(2, Strings.countMatches("abcabc", 'a'));
        assertEquals(2, Strings.countMatches("abcabc", 'c'));
        assertEquals(0, Strings.countMatches("abc", 'x'));
        assertEquals(0, Strings.countMatches(null, 'a'));
    }

    @Test
    @DisplayName("Test countMatches() with string")
    public void testCountMatches_String() {
        assertEquals(2, Strings.countMatches("abcabc", "abc"));
        assertEquals(2, Strings.countMatches("aaaa", "aa"));
        assertEquals(0, Strings.countMatches("abc", "xyz"));
        assertEquals(0, Strings.countMatches(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWith()")
    public void testStartsWith() {
        assertTrue(Strings.startsWith("abcdef", "abc"));
        assertFalse(Strings.startsWith("abcdef", "xyz"));
        assertFalse(Strings.startsWith(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWithIgnoreCase()")
    public void testStartsWithIgnoreCase() {
        assertTrue(Strings.startsWithIgnoreCase("AbCdEf", "abc"));
        assertFalse(Strings.startsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.startsWithIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWithAny()")
    public void testStartsWithAny() {
        assertTrue(Strings.startsWithAny("abcdef", "abc", "xyz"));
        assertFalse(Strings.startsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAny(null, "abc"));
    }

    @Test
    @DisplayName("Test startsWithAnyIgnoreCase()")
    public void testStartsWithAnyIgnoreCase() {
        assertTrue(Strings.startsWithAnyIgnoreCase("AbCdEf", "abc", "xyz"));
        assertFalse(Strings.startsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.startsWithAnyIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test endsWith()")
    public void testEndsWith() {
        assertTrue(Strings.endsWith("abcdef", "def"));
        assertFalse(Strings.endsWith("abcdef", "xyz"));
        assertFalse(Strings.endsWith(null, "def"));
    }

    @Test
    @DisplayName("Test endsWithIgnoreCase()")
    public void testEndsWithIgnoreCase() {
        assertTrue(Strings.endsWithIgnoreCase("AbCdEf", "def"));
        assertFalse(Strings.endsWithIgnoreCase("abcdef", "xyz"));
        assertFalse(Strings.endsWithIgnoreCase(null, "def"));
    }

    @Test
    @DisplayName("Test endsWithAny()")
    public void testEndsWithAny() {
        assertTrue(Strings.endsWithAny("abcdef", "def", "xyz"));
        assertFalse(Strings.endsWithAny("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAny(null, "def"));
    }

    @Test
    @DisplayName("Test endsWithAnyIgnoreCase()")
    public void testEndsWithAnyIgnoreCase() {
        assertTrue(Strings.endsWithAnyIgnoreCase("AbCdEf", "def", "xyz"));
        assertFalse(Strings.endsWithAnyIgnoreCase("abcdef", "xyz", "123"));
        assertFalse(Strings.endsWithAnyIgnoreCase(null, "def"));
    }

    @Test
    @DisplayName("Test equals()")
    public void testEquals() {
        assertTrue(Strings.equals("abc", "abc"));
        assertFalse(Strings.equals("abc", "ABC"));
        assertFalse(Strings.equals("abc", "xyz"));
        assertTrue(Strings.equals(null, null));
        assertFalse(Strings.equals("abc", null));
    }

    @Test
    @DisplayName("Test equalsIgnoreCase()")
    public void testEqualsIgnoreCase() {
        assertTrue(Strings.equalsIgnoreCase("abc", "ABC"));
        assertFalse(Strings.equalsIgnoreCase("abc", "xyz"));
        assertTrue(Strings.equalsIgnoreCase(null, null));
        assertFalse(Strings.equalsIgnoreCase("abc", null));
    }

    @Test
    @DisplayName("Test equalsAny()")
    public void testEqualsAny() {
        assertTrue(Strings.equalsAny("abc", "abc", "xyz"));
        assertFalse(Strings.equalsAny("abc", "xyz", "123"));
        assertFalse(Strings.equalsAny(null, "abc", "xyz"));
    }

    @Test
    @DisplayName("Test equalsAnyIgnoreCase()")
    public void testEqualsAnyIgnoreCase() {
        assertTrue(Strings.equalsAnyIgnoreCase("abc", "ABC", "xyz"));
        assertFalse(Strings.equalsAnyIgnoreCase("abc", "xyz", "123"));
        assertFalse(Strings.equalsAnyIgnoreCase(null, "abc"));
    }

    @Test
    @DisplayName("Test compareIgnoreCase()")
    public void testCompareIgnoreCase() {
        assertEquals(0, Strings.compareIgnoreCase("abc", "ABC"));
        assertTrue(Strings.compareIgnoreCase("abc", "xyz") < 0);
        assertTrue(Strings.compareIgnoreCase("xyz", "abc") > 0);
    }

    @Test
    @DisplayName("Test substring() with inclusiveBeginIndex")
    public void testSubstring_BeginIndex() {
        assertEquals("def", Strings.substring("abcdef", 3));
        assertEquals("abcdef", Strings.substring("abcdef", 0));
        assertEquals("", Strings.substring("abcdef", 6));
        assertNull(Strings.substring(null, 0));
    }

    @Test
    @DisplayName("Test substring() with begin and end index")
    public void testSubstring_BeginEndIndex() {
        assertEquals("bcd", Strings.substring("abcdef", 1, 4));
        assertEquals("abc", Strings.substring("abcdef", 0, 3));
        assertEquals("", Strings.substring("abcdef", 2, 2));
        assertNull(Strings.substring(null, 0, 3));
    }

    @Test
    @DisplayName("Test substringAfter()")
    public void testSubstringAfter() {
        assertEquals("def", Strings.substringAfter("abc:def", ':'));
        assertNull(Strings.substringAfter("abc", ':'));
        assertNull(Strings.substringAfter(null, ':'));
    }

    @Test
    @DisplayName("Test substringAfter() with string delimiter")
    public void testSubstringAfter_String() {
        assertEquals("def", Strings.substringAfter("abc::def", "::"));
        assertNull(Strings.substringAfter("abc", "::"));
        assertNull(Strings.substringAfter(null, "::"));
    }

    @Test
    @DisplayName("Test substringAfterIgnoreCase()")
    public void testSubstringAfterIgnoreCase() {
        assertEquals("DEF", Strings.substringAfterIgnoreCase("abc:DEF", ":"));
        assertNull(Strings.substringAfterIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringAfterLast()")
    public void testSubstringAfterLast() {
        assertEquals("c", Strings.substringAfterLast("a:b:c", ':'));
        assertNull(Strings.substringAfterLast("abc", ':'));
        assertNull(Strings.substringAfterLast(null, ':'));
    }

    @Test
    @DisplayName("Test substringAfterLast() with string")
    public void testSubstringAfterLast_String() {
        assertEquals("c", Strings.substringAfterLast("a::b::c", "::"));
        assertNull(Strings.substringAfterLast(null, "::"));
    }

    @Test
    @DisplayName("Test substringAfterLastIgnoreCase()")
    public void testSubstringAfterLastIgnoreCase() {
        assertEquals("C", Strings.substringAfterLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringAfterLastIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringAfterAny() with char array")
    public void testSubstringAfterAny_CharArray() {
        assertEquals("def", Strings.substringAfterAny("abcdef", 'c', 'x'));
        assertNull(Strings.substringAfterAny("abc", 'x', 'y'));
        assertNull(Strings.substringAfterAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test substringAfterAny() with string array")
    public void testSubstringAfterAny_StringArray() {
        assertEquals("def", Strings.substringAfterAny("abc::def", "::", "##"));
        assertNull(Strings.substringAfterAny(null, "::", "##"));
    }

    @Test
    @DisplayName("Test substringBefore()")
    public void testSubstringBefore() {
        assertEquals("abc", Strings.substringBefore("abc:def", ':'));
        assertNull(Strings.substringBefore("abc", ':'));
        assertNull(Strings.substringBefore(null, ':'));
    }

    @Test
    @DisplayName("Test substringBefore() with string")
    public void testSubstringBefore_String() {
        assertEquals("abc", Strings.substringBefore("abc::def", "::"));
        assertNull(Strings.substringBefore(null, "::"));
    }

    @Test
    @DisplayName("Test substringBeforeIgnoreCase()")
    public void testSubstringBeforeIgnoreCase() {
        assertEquals("abc", Strings.substringBeforeIgnoreCase("abc:DEF", ":"));
        assertNull(Strings.substringBeforeIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringBeforeLast()")
    public void testSubstringBeforeLast() {
        assertEquals("a:b", Strings.substringBeforeLast("a:b:c", ':'));
        assertNull(Strings.substringBeforeLast("abc", ':'));
        assertNull(Strings.substringBeforeLast(null, ':'));
    }

    @Test
    @DisplayName("Test substringBeforeLast() with string")
    public void testSubstringBeforeLast_String() {
        assertEquals("a::b", Strings.substringBeforeLast("a::b::c", "::"));
        assertNull(Strings.substringBeforeLast(null, "::"));
    }

    @Test
    @DisplayName("Test substringBeforeLastIgnoreCase()")
    public void testSubstringBeforeLastIgnoreCase() {
        assertEquals("a:b", Strings.substringBeforeLastIgnoreCase("a:b:C", ":"));
        assertNull(Strings.substringBeforeLastIgnoreCase(null, ":"));
    }

    @Test
    @DisplayName("Test substringBeforeAny() with char array")
    public void testSubstringBeforeAny_CharArray() {
        assertEquals("ab", Strings.substringBeforeAny("abcdef", 'c', 'x'));
        assertNull(Strings.substringBeforeAny("abc", 'x', 'y'));
        assertNull(Strings.substringBeforeAny(null, 'a', 'b'));
    }

    @Test
    @DisplayName("Test substringBeforeAny() with string array")
    public void testSubstringBeforeAny_StringArray() {
        assertEquals("abc", Strings.substringBeforeAny("abc::def", "::", "##"));
        assertNull(Strings.substringBeforeAny(null, "::", "##"));
    }

    @Test
    @DisplayName("Test substringBetween() with delimiter")
    public void testSubstringBetween() {
        assertEquals("b", Strings.substringBetween("a:b:c", ":"));
        assertNull(Strings.substringBetween("abc", ":"));
        assertNull(Strings.substringBetween(null, ":"));
    }

    @Test
    @DisplayName("Test substringBetween() with different delimiters")
    public void testSubstringBetween_DifferentDelimiters() {
        assertEquals("content", Strings.substringBetween("<tag>content</tag>", "<tag>", "</tag>"));
        assertNull(Strings.substringBetween(null, "<", ">"));
    }

    @Test
    @DisplayName("Test substringBetweenIgnoreCaes()")
    public void testSubstringBetweenIgnoreCaes() {
        assertEquals("content", Strings.substringBetweenIgnoreCaes("<TAG>content</tag>", "<tag>", "</TAG>"));
        assertNull(Strings.substringBetweenIgnoreCaes(null, "<", ">"));
    }

    @Test
    @DisplayName("Test substringsBetween()")
    public void testSubstringsBetween() {
        List<String> result = Strings.substringsBetween("a:b:c:d", ':', ':');
        assertEquals(Arrays.asList("b"), result);
    }

    @Test
    @DisplayName("Test substringIndicesBetween()")
    public void testSubstringIndicesBetween() {
        List<int[]> result = Strings.substringIndicesBetween("a:b:c", ':', ':');
        assertEquals(1, result.size());
        assertArrayEquals(new int[] { 2, 3 }, result.get(0));
    }

    @Test
    @DisplayName("Test substringOrElse()")
    public void testSubstringOrElse() {
        assertEquals("bcd", StrUtil.substringOrElse("abcdef", 1, 4, "default"));
        assertEquals("default", StrUtil.substringOrElse("abc", 10, 20, "default"));
    }

    @Test
    @DisplayName("Test substringOrElseItself()")
    public void testSubstringOrElseItself() {
        assertEquals("bcd", StrUtil.substringOrElseItself("abcdef", 1, 4));
        assertEquals("abc", StrUtil.substringOrElseItself("abc", 10, 20));
    }

    @Test
    @DisplayName("Test substringAfterOrElse()")
    public void testSubstringAfterOrElse() {
        assertEquals("def", StrUtil.substringAfterOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringAfterOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringAfterOrElseItself()")
    public void testSubstringAfterOrElseItself() {
        assertEquals("def", StrUtil.substringAfterOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringAfterOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test substringAfterLastOrElse()")
    public void testSubstringAfterLastOrElse() {
        assertEquals("c", StrUtil.substringAfterLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringAfterLastOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringAfterLastOrElseItself()")
    public void testSubstringAfterLastOrElseItself() {
        assertEquals("c", StrUtil.substringAfterLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringAfterLastOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test substringBeforeOrElse()")
    public void testSubstringBeforeOrElse() {
        assertEquals("abc", StrUtil.substringBeforeOrElse("abc:def", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringBeforeOrElseItself()")
    public void testSubstringBeforeOrElseItself() {
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc:def", ':'));
        assertEquals("abc", StrUtil.substringBeforeOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test substringBeforeLastOrElse()")
    public void testSubstringBeforeLastOrElse() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElse("a:b:c", ":", "default"));
        assertEquals("default", StrUtil.substringBeforeLastOrElse("abc", ":", "default"));
    }

    @Test
    @DisplayName("Test substringBeforeLastOrElseItself()")
    public void testSubstringBeforeLastOrElseItself() {
        assertEquals("a:b", StrUtil.substringBeforeLastOrElseItself("a:b:c", ':'));
        assertEquals("abc", StrUtil.substringBeforeLastOrElseItself("abc", ':'));
    }

    @Test
    @DisplayName("Test commonPrefix() with two strings")
    public void testCommonPrefix() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz"));
        assertEquals("", Strings.commonPrefix("abc", "xyz"));
        assertEquals("", Strings.commonPrefix(null, "abc"));
    }

    @Test
    @DisplayName("Test commonPrefix() with multiple strings")
    public void testCommonPrefix_Multiple() {
        assertEquals("abc", Strings.commonPrefix("abcdef", "abcxyz", "abc123"));
        assertEquals("", Strings.commonPrefix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test commonSuffix() with two strings")
    public void testCommonSuffix() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz"));
        assertEquals("", Strings.commonSuffix(null, "abc"));
    }

    @Test
    @DisplayName("Test commonSuffix() with multiple strings")
    public void testCommonSuffix_Multiple() {
        assertEquals("xyz", Strings.commonSuffix("abcxyz", "defxyz", "123xyz"));
        assertEquals("", Strings.commonSuffix("abc", "xyz", "123"));
    }

    @Test
    @DisplayName("Test lengthOfCommonPrefix()")
    public void testLengthOfCommonPrefix() {
        assertEquals(3, Strings.lengthOfCommonPrefix("abcdef", "abcxyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonPrefix(null, "abc"));
    }

    @Test
    @DisplayName("Test lengthOfCommonSuffix()")
    public void testLengthOfCommonSuffix() {
        assertEquals(3, Strings.lengthOfCommonSuffix("abcxyz", "defxyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix("abc", "xyz"));
        assertEquals(0, Strings.lengthOfCommonSuffix(null, "abc"));
    }

    @Test
    @DisplayName("Test longestCommonSubstring()")
    public void testLongestCommonSubstring() {
        assertEquals("abc", Strings.longestCommonSubstring("xabcy", "zabc123"));
        assertEquals("", Strings.longestCommonSubstring("abc", "xyz"));
        assertEquals("", Strings.longestCommonSubstring(null, "abc"));
    }

    @Test
    @DisplayName("Test firstChar()")
    public void testFirstChar() {
        assertTrue(Strings.firstChar("abc").isPresent());
        assertEquals('a', Strings.firstChar("abc").get());
        assertFalse(Strings.firstChar("").isPresent());
        assertFalse(Strings.firstChar(null).isPresent());
    }

    @Test
    @DisplayName("Test lastChar()")
    public void testLastChar() {
        assertTrue(Strings.lastChar("abc").isPresent());
        assertEquals('c', Strings.lastChar("abc").get());
        assertFalse(Strings.lastChar("").isPresent());
        assertFalse(Strings.lastChar(null).isPresent());
    }

    @Test
    @DisplayName("Test firstChars()")
    public void testFirstChars() {
        assertEquals("ab", Strings.firstChars("abcdef", 2));
        assertEquals("abcdef", Strings.firstChars("abcdef", 10));
        assertEquals("", Strings.firstChars(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.firstChars("abc", -1));
    }

    @Test
    @DisplayName("Test lastChars()")
    public void testLastChars() {
        assertEquals("ef", Strings.lastChars("abcdef", 2));
        assertEquals("abcdef", Strings.lastChars("abcdef", 10));
        assertEquals("", Strings.lastChars(null, 2));
        assertThrows(IllegalArgumentException.class, () -> Strings.lastChars("abc", -1));
    }

    @Test
    @DisplayName("Test toCodePoints()")
    public void testToCodePoints() {
        int[] codePoints = Strings.toCodePoints("abc");
        assertArrayEquals(new int[] { 'a', 'b', 'c' }, codePoints);
        assertArrayEquals(new int[0], Strings.toCodePoints(""));
        assertNull(Strings.toCodePoints(null));
    }

    @Test
    @DisplayName("Test concat() with multiple strings")
    public void testConcat() {
        assertEquals("abcdef", Strings.concat("abc", "def"));
        assertEquals("abc", Strings.concat("abc", null));
        assertEquals("def", Strings.concat(null, "def"));
        assertEquals("", Strings.concat((String[]) null));
    }

    @Test
    @DisplayName("Test join() with array")
    public void testJoin() {
        assertEquals("a,b,c", Strings.join(new String[] { "a", "b", "c" }, ","));
        assertEquals("abc", Strings.join(new String[] { "a", "b", "c" }, ""));
        assertEquals("", Strings.join((String[]) null, ","));
    }

    @Test
    @DisplayName("Test join() with Iterable")
    public void testJoin_Iterable() {
        assertEquals("a,b,c", Strings.join(Arrays.asList("a", "b", "c"), ","));
        assertEquals("", Strings.join(new ArrayList<>(), ","));
        assertEquals("", Strings.join((Iterable<String>) null, ","));
    }

    @Test
    @DisplayName("Test joinEntries()")
    public void testJoinEntries() {
        Map<String, String> map = N.asLinkedHashMap("a", "1", "b", "2");
        String result = Strings.joinEntries(map, ",", "=");
        assertEquals("a=1,b=2", result);
    }

    @Test
    @DisplayName("Test parseInt()")
    public void testParseInt() {
        assertEquals(123, Strings.parseInt("123"));
        assertEquals(-456, Strings.parseInt("-456"));
        assertEquals(0, Strings.parseInt(null));
        assertEquals(0, Strings.parseInt(""));
        assertThrows(NumberFormatException.class, () -> Strings.parseInt("abc"));
    }

    @Test
    @DisplayName("Test parseLong()")
    public void testParseLong() {
        assertEquals(123L, Strings.parseLong("123"));
        assertEquals(-456L, Strings.parseLong("-456"));
        assertThrows(NumberFormatException.class, () -> Strings.parseLong("abc"));
    }

    @Test
    @DisplayName("Test parseDouble()")
    public void testParseDouble() {
        assertEquals(123.45, Strings.parseDouble("123.45"), 0.001);
        assertEquals(-456.78, Strings.parseDouble("-456.78"), 0.001);
        assertThrows(NumberFormatException.class, () -> Strings.parseDouble("abc"));
    }

    @Test
    @DisplayName("Test parseFloat()")
    public void testParseFloat() {
        assertEquals(123.45f, Strings.parseFloat("123.45"), 0.001);
        assertEquals(-456.78f, Strings.parseFloat("-456.78"), 0.001);
        assertThrows(NumberFormatException.class, () -> Strings.parseFloat("abc"));
    }

    @Test
    @DisplayName("Test parseBoolean()")
    public void testParseBoolean() {
        assertTrue(Strings.parseBoolean("true"));
        assertFalse(Strings.parseBoolean("false"));
        assertFalse(Strings.parseBoolean("abc"));
        assertFalse(Strings.parseBoolean(null));
    }

    @Test
    @DisplayName("Test parseByte()")
    public void testParseByte() {
        assertEquals((byte) 123, Strings.parseByte("123"));
        assertEquals((byte) -45, Strings.parseByte("-45"));
        assertThrows(NumberFormatException.class, () -> Strings.parseByte("abc"));
    }

    @Test
    @DisplayName("Test parseShort()")
    public void testParseShort() {
        assertEquals((short) 123, Strings.parseShort("123"));
        assertEquals((short) -456, Strings.parseShort("-456"));
        assertThrows(NumberFormatException.class, () -> Strings.parseShort("abc"));
    }

    @Test
    @DisplayName("Test parseChar()")
    public void testParseChar() {
        assertEquals('a', Strings.parseChar("a"));
        assertThrows(IllegalArgumentException.class, () -> Strings.parseChar("abc"));
        assertEquals('\0', Strings.parseChar(null));
    }

    @Test
    @DisplayName("Test createNumber()")
    public void testCreateNumber() {
        assertEquals(123, StrUtil.createNumber("123").orElseThrow());
        assertEquals(123.45d, StrUtil.createNumber("123.45").orElseThrow());
        assertEquals(123L, StrUtil.createNumber("123L").orElseThrow());
        assertTrue(StrUtil.createNumber(null).isEmpty());
    }

    @Test
    @DisplayName("Test createInteger()")
    public void testCreateInteger() {
        assertEquals(Integer.valueOf(123), StrUtil.createInteger("123").orElseThrow());
        assertTrue(StrUtil.createInteger(null).isEmpty());
        assertTrue(StrUtil.createInteger("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createLong()")
    public void testCreateLong() {
        assertEquals(Long.valueOf(123), StrUtil.createLong("123").orElseThrow());
        assertTrue(StrUtil.createLong(null).isEmpty());
        assertTrue(StrUtil.createLong("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createDouble()")
    public void testCreateDouble() {
        assertEquals(Double.valueOf(123.45), StrUtil.createDouble("123.45").orElseThrow());
        assertTrue(StrUtil.createDouble(null).isEmpty());
        assertTrue(StrUtil.createDouble("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createFloat()")
    public void testCreateFloat() {
        assertEquals(Float.valueOf(123.45f), StrUtil.createFloat("123.45").orElseThrow());
        assertTrue(StrUtil.createFloat(null).isEmpty());
        assertTrue(StrUtil.createFloat("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createBigInteger()")
    public void testCreateBigInteger() {
        assertEquals(new BigInteger("123"), StrUtil.createBigInteger("123").orElseThrow());
        assertTrue(StrUtil.createBigInteger(null).isEmpty());
        assertTrue(StrUtil.createBigInteger("abc").isEmpty());
    }

    @Test
    @DisplayName("Test createBigDecimal()")
    public void testCreateBigDecimal() {
        assertEquals(new BigDecimal("123.45"), StrUtil.createBigDecimal("123.45").orElseThrow());
        assertTrue(StrUtil.createBigDecimal(null).isEmpty());
        assertTrue(StrUtil.createBigDecimal("abc").isEmpty());
    }

    @Test
    @DisplayName("Test extractFirstInteger()")
    public void testExtractFirstInteger() {
        assertEquals("123", Strings.extractFirstInteger("abc123def"));
        assertNull(Strings.extractFirstInteger("abc"));
        assertNull(Strings.extractFirstInteger(null));
    }

    @Test
    @DisplayName("Test extractFirstDouble()")
    public void testExtractFirstDouble() {
        assertEquals("123.45", Strings.extractFirstDouble("abc123.45def"));
        assertNull(Strings.extractFirstDouble("abc"));
        assertNull(Strings.extractFirstDouble(null));
    }

    @Test
    @DisplayName("Test findFirstEmailAddress()")
    public void testFindFirstEmailAddress() {
        assertEquals("test@example.com", Strings.findFirstEmailAddress("Contact: test@example.com"));
        assertNull(Strings.findFirstEmailAddress("No email here"));
        assertNull(Strings.findFirstEmailAddress(null));
    }

    @Test
    @DisplayName("Test findAllEmailAddresses()")
    public void testFindAllEmailAddresses() {
        List<String> emails = Strings.findAllEmailAddresses("Contact: test@example.com and admin@test.org");
        assertEquals(2, emails.size());
        assertTrue(emails.contains("test@example.com"));
        assertTrue(emails.contains("admin@test.org"));
    }

    @Test
    @DisplayName("Test reverse()")
    public void testReverse() {
        assertEquals("cba", Strings.reverse("abc"));
        assertEquals("", Strings.reverse(""));
        assertNull(Strings.reverse(null));
    }

    @Test
    @DisplayName("Test reverseDelimited()")
    public void testReverseDelimited() {
        assertEquals("c,b,a", Strings.reverseDelimited("a,b,c", ','));
        assertNull(Strings.reverseDelimited(null, ','));
    }

    @Test
    @DisplayName("Test rotate()")
    public void testRotate() {
        assertEquals("cab", Strings.rotate("abc", 1));
        assertEquals("bca", Strings.rotate("abc", -1));
        assertEquals("abc", Strings.rotate("abc", 3));
        assertNull(Strings.rotate(null, 1));
    }

    @Test
    @DisplayName("Test shuffle()")
    public void testShuffle() {
        String shuffled = Strings.shuffle("abcdef");
        assertNotNull(shuffled);
        assertEquals(6, shuffled.length());
        assertNull(Strings.shuffle(null));
    }

    @Test
    @DisplayName("Test sort()")
    public void testSort() {
        assertEquals("abc", Strings.sort("cba"));
        assertEquals("", Strings.sort(""));
        assertNull(Strings.sort(null));
    }

    @Test
    @DisplayName("Test overlay()")
    public void testOverlay() {
        assertEquals("abXYZfg", Strings.overlay("abcdefg", "XYZ", 2, 5));
        assertEquals("XYZ", Strings.overlay(null, "XYZ", 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.overlay(null, "XYZ", 0, 1));
    }

    @Test
    @DisplayName("Test deleteRange()")
    public void testDeleteRange() {
        assertEquals("abfg", Strings.deleteRange("abcdefg", 2, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.deleteRange(null, 0, 1));
    }

    @Test
    @DisplayName("Test moveRange()")
    public void testMoveRange() {
        String result = Strings.moveRange("abcdefg", 2, 5, 0);
        assertNotNull(result);
        assertEquals("", Strings.moveRange(null, 0, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.moveRange(null, 0, 1, 2));
    }

    @Test
    @DisplayName("Test replaceRange()")
    public void testReplaceRange() {
        assertEquals("abXYZfg", Strings.replaceRange("abcdefg", 2, 5, "XYZ"));
        assertEquals("X", Strings.replaceRange("", 0, 0, "X"));
        assertEquals("X", Strings.replaceRange(null, 0, 0, "X"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.replaceRange(null, 0, 1, "X"));
    }

    @Test
    @DisplayName("Test replaceFirstInteger()")
    public void testReplaceFirstInteger() {
        assertEquals("abcXYZdef", Strings.replaceFirstInteger("abc123def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstInteger("abc", "XYZ"));
        assertEquals("", Strings.replaceFirstInteger(null, "XYZ"));
    }

    @Test
    @DisplayName("Test replaceFirstDouble()")
    public void testReplaceFirstDouble() {
        assertEquals("abcXYZdef", Strings.replaceFirstDouble("abc123.45def", "XYZ"));
        assertEquals("abc", Strings.replaceFirstDouble("abc", "XYZ"));
        assertEquals("", Strings.replaceFirstDouble(null, "XYZ"));
    }

    @Test
    @DisplayName("Test replaceIgnoreCase()")
    public void testReplaceIgnoreCase() {
        assertEquals("xyzxyz", Strings.replaceIgnoreCase("AbCABC", 0, "abc", "xyz", 2));
        assertNull(Strings.replaceIgnoreCase(null, 0, "abc", "xyz", 1));
    }

    @Test
    @DisplayName("Test copyThenTrim()")
    public void testCopyThenTrim() {
        String[] result = Strings.copyThenTrim(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenTrim((String[]) null));
    }

    @Test
    @DisplayName("Test copyThenStrip()")
    public void testCopyThenStrip() {
        String[] result = Strings.copyThenStrip(new String[] { "  a  ", " b " });
        assertArrayEquals(new String[] { "a", "b" }, result);
        assertNull(Strings.copyThenStrip((String[]) null));
    }

    @Test
    @DisplayName("Test isAsciiDigitalInteger()")
    public void testIsAsciiDigitalInteger() {
        assertTrue(Strings.isAsciiDigitalInteger("123"));
        assertTrue(Strings.isAsciiDigitalInteger("-123"));
        assertTrue(Strings.isAsciiDigitalInteger("+123"));
        assertFalse(Strings.isAsciiDigitalInteger("12.3"));
        assertFalse(Strings.isAsciiDigitalInteger("abc"));
        assertFalse(Strings.isAsciiDigitalInteger(null));
        assertFalse(Strings.isAsciiDigitalInteger(""));
    }

    @Test
    @DisplayName("Test lenientFormat()")
    public void testLenientFormat() {
        assertEquals("Hello World", Strings.lenientFormat("Hello %s", "World"));
        assertEquals("Value: 123", Strings.lenientFormat("Value: %s", 123));
        assertNotNull(Strings.lenientFormat("Test %s %s", "a"));
    }
}
