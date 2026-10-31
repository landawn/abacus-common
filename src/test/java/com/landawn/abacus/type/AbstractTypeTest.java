package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlParser;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.TypeReference;

public class AbstractTypeTest extends TestBase {

    @Test
    public void temporalCharacterSlicesRejectLongHexadecimalTokens() {
        final Class<?>[] temporalClasses = { java.time.Instant.class, java.time.OffsetDateTime.class, java.time.ZonedDateTime.class, java.util.Date.class,
                java.sql.Date.class, java.sql.Time.class, java.sql.Timestamp.class, java.util.Calendar.class, java.util.GregorianCalendar.class,
                javax.xml.datatype.XMLGregorianCalendar.class, org.joda.time.DateTime.class, org.joda.time.Instant.class, org.joda.time.MutableDateTime.class };

        for (final Class<?> temporalClass : temporalClasses) {
            final Type<?> type = Type.of(temporalClass);

            // These tokens reach the general numeric parser's hexadecimal path when their length exceeds 18.
            for (final String token : new String[] { "0x000001234567890123", "-0x000001234567890123", "+0X000001234567890123", "#0000001234567890123" }) {
                final char[] wrapped = ("prefix" + token + "suffix").toCharArray();
                final RuntimeException stringFailure = assertThrows(RuntimeException.class, () -> type.valueOf(token), temporalClass.getName() + ": " + token);
                final RuntimeException sliceFailure = assertThrows(RuntimeException.class, () -> type.valueOf(wrapped, 6, token.length()),
                        temporalClass.getName() + ": " + token);
                assertEquals(stringFailure.getClass(), sliceFailure.getClass(), temporalClass.getName() + ": " + token);
            }

            for (final String token : new String[] { "1700000000000", "+1700000000000", "-1700000000000", "0000001700000000000" }) {
                final char[] wrapped = ("prefix" + token + "suffix").toCharArray();
                assertEquals(type.valueOf(token), type.valueOf(wrapped, 6, token.length()), temporalClass.getName() + ": " + token);
            }
        }
    }

    @Test
    public void numericTypesAcceptOnlyOneLiteralSuffix() {
        for (final Class<?> numericClass : new Class<?>[] { byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class,
                float.class, Float.class, double.class, Double.class }) {
            final Type<?> type = Type.of(numericClass);
            for (final String token : new String[] { "1LL", "1Ll", "1Lf", "1LD", "1ff", "1fD", "1dd", "1dF", "1dL", "1 F", "1F L", "1D\tF" }) {
                assertThrows(NumberFormatException.class, () -> type.valueOf(token), numericClass.getName() + ": " + token);
                final char[] wrapped = ("prefix" + token + "suffix").toCharArray();
                assertThrows(NumberFormatException.class, () -> type.valueOf(wrapped, 6, token.length()), numericClass.getName() + ": " + token);
            }
            for (final String token : new String[] { "1", "1L", "1l", "1F", "1f", "1D", "1d" }) {
                assertEquals(1.0, ((Number) type.valueOf(token)).doubleValue(), numericClass.getName() + ": " + token);
                final char[] wrapped = ("prefix" + token + "suffix").toCharArray();
                assertEquals(type.valueOf(token), type.valueOf(wrapped, 6, token.length()), numericClass.getName() + ": " + token);
            }
        }

        for (final Class<?> numericClass : new Class<?>[] { Byte.class, Short.class, Integer.class, Long.class }) {
            final Type<?> type = Type.of(numericClass);
            assertEquals(31, ((Number) type.valueOf("0x1F")).intValue());
            assertEquals(31, ((Number) type.valueOf("0x1FL")).intValue());
        }
        assertEquals(15.5, Type.<Double> of(Double.class).valueOf("0x1.fp3"));
    }

    private Type<String> stringType;
    private Type<Integer> integerType;
    private Type<List<String>> listType;

    private static class TestComparableType extends AbstractType<String> {
        public TestComparableType() {
            super("TestComparableType");
        }

        @Override
        public Class<String> javaType() {
            return String.class;
        }

        @Override
        public boolean isComparable() {
            return true;
        }

        @Override
        public String stringOf(String x) {
            return x;
        }

        @Override
        public String valueOf(String str) {
            return str;
        }
    }

    private static class TestSplitType extends AbstractType<String> {
        public TestSplitType() {
            super("TestSplitType");
        }

        @Override
        public Class<String> javaType() {
            return String.class;
        }

        @Override
        public String stringOf(String x) {
            return x;
        }

        @Override
        public String valueOf(String str) {
            return str;
        }

        public String[] splitForTest(String str, String sep) {
            return AbstractType.split(str, sep);
        }

        public String[] getTypeParametersForTest(String typeName) {
            return AbstractType.getTypeParameters(typeName);
        }

        public String[] getParametersForTest(String typeName) {
            return AbstractType.getParameters(typeName);
        }
    }

    @BeforeEach
    public void setUp() {
        stringType = createType(String.class);
        integerType = createType(Integer.class);
        listType = createType("List<String>");
    }

    @Test
    public void testNameDeclaringNameXmlName() {
        assertEquals("String", stringType.name());
        assertEquals("String", stringType.xmlName());
        assertEquals("List<String>", listType.declaringName());
        assertEquals("Map&lt;String, Integer&gt;", createType("Map<String,Integer>").xmlName());
        assertEquals("String", stringType.toString());
    }

    @Test
    public void testTypeFlagsAndDefaults() {
        assertFalse(stringType.isPrimitive());
        assertFalse(stringType.isPrimitiveWrapper());
        assertFalse(stringType.isPrimitiveList());
        assertFalse(stringType.isBoolean());
        assertFalse(stringType.isNumber());
        assertFalse(integerType.isString());
        assertFalse(integerType.isCharSequence());
        assertFalse(stringType.isDate());
        assertFalse(stringType.isCalendar());
        assertFalse(stringType.isJodaDateTime());
        assertFalse(stringType.isTemporal());
        assertFalse(stringType.isPrimitiveArray());
        assertFalse(stringType.isPrimitiveByteArray());
        assertFalse(stringType.isObjectArray());
        assertFalse(stringType.isArray());
        assertFalse(stringType.isList());
        assertFalse(stringType.isSet());
        assertFalse(stringType.isCollection());
        assertFalse(stringType.isMap());
        assertFalse(stringType.isBean());
        assertFalse(stringType.isMapEntity());
        assertFalse(stringType.isEntityId());
        assertFalse(stringType.isDataset());
        assertFalse(stringType.isInputStream());
        assertFalse(stringType.isReader());
        assertFalse(stringType.isByteBuffer());
        assertFalse(stringType.isParameterizedType());
        assertTrue(stringType.isImmutable());
        assertTrue(stringType.isComparable());
        assertTrue(new TestSplitType().isComparable());
        assertTrue(stringType.isSerializable());
        assertFalse(stringType.isOptionalOrNullable());
        assertFalse(stringType.isObject());
        assertNull(stringType.defaultValue());
        assertTrue(stringType.isDefaultValue(null));
        assertFalse(stringType.isDefaultValue("test"));
        assertEquals(Type.SerializationType.SERIALIZABLE, stringType.serializationType());
        assertNull(stringType.elementType());
        assertEquals(0, stringType.parameterTypes().size());
    }

    @Test
    public void testCompareValueOfAndEquals() {
        TestComparableType comparableType = new TestComparableType();
        assertEquals(0, comparableType.compare("a", "a"));
        assertTrue(comparableType.compare("a", "b") < 0);
        assertTrue(comparableType.compare("b", "a") > 0);
        assertEquals(0, comparableType.compare(null, null));
        assertTrue(comparableType.compare(null, "a") < 0);
        assertTrue(comparableType.compare("a", null) > 0);

        assertEquals("test", stringType.valueOf((Object) "test"));
        assertNull(stringType.valueOf((Object) null));
        char[] chars = "hello world".toCharArray();
        assertEquals("hello", stringType.valueOf(chars, 0, 5));
        assertEquals("world", stringType.valueOf(chars, 6, 5));
        assertNull(stringType.valueOf(null, 0, 0));

        assertEquals(stringType.name().hashCode(), stringType.hashCode());
        assertEquals("test".hashCode(), stringType.hashCode("test"));
        assertEquals(0, stringType.hashCode(null));
        assertEquals("test".hashCode(), stringType.deepHashCode("test"));
        assertEquals(0, stringType.deepHashCode(null));
        assertTrue(stringType.equals(createType(String.class)));
        assertFalse(stringType.equals(integerType));
        assertFalse(stringType.equals("String"));
        assertTrue(stringType.equals("test", "test"));
        assertFalse(stringType.equals("test1", "test2"));
        assertTrue(stringType.equals(null, null));
        assertFalse(stringType.equals("test", null));
        assertFalse(stringType.equals(null, "test"));
        assertTrue(stringType.deepEquals("test", "test"));
        assertFalse(stringType.deepEquals("test1", "test2"));
        assertTrue(stringType.deepEquals(null, null));
        assertFalse(stringType.deepEquals("test", null));
        assertEquals("test", stringType.toString("test"));
        assertEquals("null", stringType.toString(null));
        assertEquals("test", stringType.deepToString("test"));
        assertEquals("null", stringType.deepToString(null));
    }

    @Test
    public void testJdbcGetSetAppendAndSerialize() throws SQLException, IOException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");
        when(rs.getString("column")).thenReturn("test");
        assertEquals("test", stringType.get(rs, 1));
        assertEquals("test", stringType.get(rs, "column"));
        verify(rs).getString(1);
        verify(rs).getString("column");

        PreparedStatement stmt = mock(PreparedStatement.class);
        stringType.set(stmt, 1, "test");
        stringType.set(stmt, 2, null);
        stringType.set(stmt, 3, "test", java.sql.Types.VARCHAR);
        verify(stmt).setString(1, "test");
        verify(stmt).setString(2, null);
        verify(stmt).setString(3, "test");

        CallableStatement callable = mock(CallableStatement.class);
        stringType.set(callable, "param", "test");
        stringType.set(callable, "param2", null);
        stringType.set(callable, "param3", "test", java.sql.Types.VARCHAR);
        verify(callable).setString("param", "test");
        verify(callable).setString("param2", null);
        verify(callable).setString("param3", "test");

        StringBuilder sb = new StringBuilder();
        stringType.appendTo(sb, "test");
        assertEquals("test", sb.toString());
        sb = new StringBuilder();
        stringType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        CharacterWriter writer = mock(BufferedJsonWriter.class);
        stringType.serializeTo(writer, "test", null);
        verify(writer).writeCharacter("test");
        stringType.serializeTo(writer, null, null);
        verify(writer).write("null".toCharArray());
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getStringQuotation()).thenReturn('"');
        stringType.serializeTo(writer, "test", config);
        verify(writer, times(2)).write('"');

        BufferedJsonWriter jsonWriter = mock(BufferedJsonWriter.class);
        new TestSplitType().serializeTo(jsonWriter, null, null);
        verify(jsonWriter, times(1)).write(AbstractType.NULL_CHAR_ARRAY);

        assertThrows(UnsupportedOperationException.class, () -> stringType.collectionToArray(Arrays.asList("a", "b", "c")));
        assertThrows(UnsupportedOperationException.class, () -> stringType.arrayToCollection("test", ArrayList.class));
        assertThrows(UnsupportedOperationException.class, () -> stringType.arrayToCollection("test", new ArrayList<>()));
    }

    @Test
    public void testSplitAndTypeParameterHelpers() {
        TestSplitType helper = new TestSplitType();
        String[] typeParams = helper.getTypeParametersForTest("Map<String, Integer>");
        assertEquals(2, typeParams.length);
        assertEquals("String", typeParams[0]);
        assertEquals("Integer", typeParams[1]);
        String[] params = helper.getParametersForTest("HashMap<String, Integer>(16, 0.75f)");
        assertEquals(2, params.length);
        assertEquals("16", params[0]);
        assertEquals("0.75f", params[1]);

        String[] parts = helper.splitForTest("a,b,c", ",");
        assertEquals(3, parts.length);
        assertEquals("a", parts[0]);
        assertEquals("c", parts[2]);
        String[] pipes = helper.splitForTest("x|y", "\\|");
        assertEquals(2, pipes.length);
        assertEquals(List.of("a", "b", "c"), Arrays.asList(helper.splitForTest("a[b[c", "[")));
        assertEquals(List.of("a", "b", "c"), Arrays.asList(helper.splitForTest("a]b]c", "]")));
        assertEquals(List.of("a", "b", "c"), Arrays.asList(helper.splitForTest("a(b(c", "(")));
        assertEquals(List.of("a", "b", "c"), Arrays.asList(helper.splitForTest("a)b)c", ")")));
        assertEquals(List.of("a", "b", "c"), Arrays.asList(helper.splitForTest("a{b{c", "{")));
        assertEquals(List.of("a", "b", "c"), Arrays.asList(helper.splitForTest("a}b}c", "}")));
    }

    @Test
    public void testParseIntLongChar() {
        assertEquals(12345, AbstractType.parseInt("__+12345L".toCharArray(), 2, 7));
        assertEquals(-42, AbstractType.parseInt("x-42".toCharArray(), 1, 3));
        assertEquals(1234567890, AbstractType.parseInt("1234567890".toCharArray(), 0, 10));
        assertEquals(0, AbstractType.parseInt(null, 0, 0));
        assertEquals(0, AbstractType.parseInt("abc".toCharArray(), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseInt("123".toCharArray(), -1, 3));
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseInt("123".toCharArray(), 0, -1));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("L".toCharArray(), 0, 1));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("x".toCharArray(), 0, 1));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12x".toCharArray(), 0, 3));

        assertEquals(0L, AbstractType.parseLong(null, 0, 0));
        assertEquals(0L, AbstractType.parseLong("abc".toCharArray(), 0, 0));
        assertEquals(9876543210123L, AbstractType.parseLong("__9876543210123d".toCharArray(), 2, 14));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("L".toCharArray(), 0, 1));
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseLong("123".toCharArray(), -1, 3));
        assertEquals(0L, AbstractType.parseLong("0f".toCharArray(), 0, 2));

        assertEquals('A', AbstractType.parseChar("A"));
        assertEquals('A', AbstractType.parseChar("65"));
        assertEquals('\n', AbstractType.parseChar("10"));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseChar("abc"));
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseChar("-1"));
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseChar("65536"));
    }

    @Test
    public void reviewFixes20260906_parseIntAndParseLongReportOverflowAsArithmeticException() {
        // T1-09 (documentation pin): well-formed digits outside the range go through Numbers.toInt/toLong,
        // which report overflow as ArithmeticException, not NumberFormatException.
        assertEquals(Integer.MAX_VALUE, AbstractType.parseInt("2147483647".toCharArray(), 0, 10));
        assertEquals(Integer.MIN_VALUE, AbstractType.parseInt("-2147483648".toCharArray(), 0, 11));
        assertThrows(ArithmeticException.class, () -> AbstractType.parseInt("2147483648".toCharArray(), 0, 10));
        assertThrows(ArithmeticException.class, () -> AbstractType.parseInt("-2147483649".toCharArray(), 0, 11));
        assertThrows(ArithmeticException.class, () -> AbstractType.parseInt("99999999999".toCharArray(), 0, 11));

        assertEquals(Long.MAX_VALUE, AbstractType.parseLong("9223372036854775807".toCharArray(), 0, 19));
        assertEquals(Long.MIN_VALUE, AbstractType.parseLong("-9223372036854775808".toCharArray(), 0, 20));
        assertThrows(ArithmeticException.class, () -> AbstractType.parseLong("9223372036854775808".toCharArray(), 0, 19));
        assertThrows(ArithmeticException.class, () -> AbstractType.parseLong("-9223372036854775809".toCharArray(), 0, 20));

        // malformed digits stay NumberFormatException on both paths
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12x".toCharArray(), 0, 3));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12345678x9".toCharArray(), 0, 10));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("1234567890123456789x".toCharArray(), 0, 20));
    }

    // ---- bug review 2026-09-27 G114 begin ----

    // G114-05: a malformed token ending in a type-suffix letter reported only that letter ("null" -> "l").
    @Test
    public void testParseIntAndParseLong_suffixErrorMessageNamesWholeToken() {
        assertEquals("Invalid numeric String: \"null\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("[null]".toCharArray(), 1, 4)).getMessage());
        assertEquals("Invalid numeric String: \"12aL\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12aL".toCharArray(), 0, 4)).getMessage());
        assertEquals("Invalid numeric String: \"L\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("L".toCharArray(), 0, 1)).getMessage());
        assertEquals("Invalid numeric String: \"null\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("[null]".toCharArray(), 1, 4)).getMessage());
        assertEquals("Invalid numeric String: \"-d\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("-d".toCharArray(), 0, 2)).getMessage());
    }

    // ---- bug review 2026-09-27 G114 end ----

    // ---- bug review 2026-09-27 verify G119 begin ----

    // parseInt/parseLong(char[]): a suffixed token rejected by the digit loop also names the whole token; unsuffixed tokens unchanged.
    @Test
    public void testParseIntAndParseLong_suffixedTokenRejectedInDigitLoopNamesWholeToken() {
        assertEquals("Invalid numeric String: \"a1L\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("a1L".toCharArray(), 0, 3)).getMessage());
        assertEquals("Invalid numeric String: \"1-2d\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("[1-2d]".toCharArray(), 1, 4)).getMessage());
        assertEquals("Invalid numeric String: \"x1\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("x1".toCharArray(), 0, 2)).getMessage());
        assertEquals("Invalid numeric String: \"x\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("x".toCharArray(), 0, 1)).getMessage());
    }

    // parseInt/parseLong(char[]): valid tokens, with or without a suffix, at an offset and on the long fallback path, still parse.
    @Test
    public void testParseIntAndParseLong_validSuffixedTokensUnaffected() {
        assertEquals(12, AbstractType.parseInt("[12L]".toCharArray(), 1, 3));
        assertEquals(-5, AbstractType.parseInt("-5d".toCharArray(), 0, 3));
        assertEquals(7, AbstractType.parseInt("7F".toCharArray(), 0, 2));
        assertEquals(1234567890, AbstractType.parseInt("1234567890L".toCharArray(), 0, 11));
        assertEquals(1234567890123L, AbstractType.parseLong("x1234567890123Lx".toCharArray(), 1, 14));
        assertEquals(-3L, AbstractType.parseLong("-3l".toCharArray(), 0, 3));
        assertEquals(42L, AbstractType.parseLong("42".toCharArray(), 0, 2));
    }

    // ---- bug review 2026-09-27 verify G119 end ----

    // ---- bug review 2026-09-27 verify G123 begin ----

    // G123-03: a suffixed token long enough for the Numbers.toInt/toLong fallback reported the token without its suffix.
    @Test
    public void testParseIntAndParseLong_suffixedFallbackTokenErrorMessageNamesWholeToken() {
        assertEquals("Invalid numeric String: \"12345678x9L\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12345678x9L".toCharArray(), 0, 11)).getMessage());
        assertEquals("Invalid numeric String: \"12345678.5d\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("[12345678.5d]".toCharArray(), 1, 11)).getMessage());
        assertEquals("Invalid numeric String: \"123456789012345678x9L\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("123456789012345678x9L".toCharArray(), 0, 21)).getMessage());
        assertEquals("Invalid numeric String: \"123456789012345678.5f\"",
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("[123456789012345678.5f]".toCharArray(), 1, 21)).getMessage());
    }

    // G123-03 neighbours: unsuffixed fallback failures keep the Numbers message, overflow stays ArithmeticException (suffix or not),
    // and valid suffixed fallback tokens still parse.
    @Test
    public void testParseIntAndParseLong_fallbackPathOtherwiseUnchanged() {
        assertEquals(assertThrows(NumberFormatException.class, () -> com.landawn.abacus.util.Numbers.toInt("12345678x9")).getMessage(),
                assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12345678x9".toCharArray(), 0, 10)).getMessage());
        assertEquals(assertThrows(NumberFormatException.class, () -> com.landawn.abacus.util.Numbers.toLong("1234567890123456789x")).getMessage(),
                assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("1234567890123456789x".toCharArray(), 0, 20)).getMessage());

        assertEquals(assertThrows(ArithmeticException.class, () -> com.landawn.abacus.util.Numbers.toInt("2147483648")).getMessage(),
                assertThrows(ArithmeticException.class, () -> AbstractType.parseInt("2147483648L".toCharArray(), 0, 11)).getMessage());
        assertThrows(ArithmeticException.class, () -> AbstractType.parseInt("-2147483649d".toCharArray(), 0, 12));
        assertEquals(assertThrows(ArithmeticException.class, () -> com.landawn.abacus.util.Numbers.toLong("9223372036854775808")).getMessage(),
                assertThrows(ArithmeticException.class, () -> AbstractType.parseLong("9223372036854775808L".toCharArray(), 0, 20)).getMessage());
        assertThrows(ArithmeticException.class, () -> AbstractType.parseLong("9223372036854775808".toCharArray(), 0, 19));

        assertEquals(1234567890, AbstractType.parseInt("[1234567890L]".toCharArray(), 1, 11));
        assertEquals(-1234567890, AbstractType.parseInt("-1234567890d".toCharArray(), 0, 12));
        assertEquals(1234567890123456789L, AbstractType.parseLong("1234567890123456789L".toCharArray(), 0, 20));
        assertEquals(Long.MIN_VALUE, AbstractType.parseLong("-9223372036854775808l".toCharArray(), 0, 21));
    }

    // ---- bug review 2026-09-27 verify G123 end ----

    // ---- deep review 2026-10-27 G134 begin ----

    // Every public class in java.lang.* / java.util.* / java.time.* / com.landawn.abacus.* (all compiled main classes and
    // all JDK modules enumerated) whose simple name ClassUtil resolves to a DIFFERENT class, and whose Type name is built
    // from its canonical name. name() is written as type information, so it must name the class itself.
    private static final Class<?>[] G134_SIMPLE_NAME_IS_ANOTHER_CLASS = { //
            com.landawn.abacus.annotation.Record.class, // "Record" is java.lang.Record
            com.landawn.abacus.annotation.Type.class, // "Type" is com.landawn.abacus.type.Type
            com.landawn.abacus.util.Multiset.Entry.class, // "Entry" is java.util.Map.Entry
            com.landawn.abacus.util.Throwables.Iterator.class, // "Iterator" is java.util.Iterator
            com.landawn.abacus.util.Throwables.Runnable.class, // "Runnable" is java.lang.Runnable (also the next three)
            com.landawn.abacus.util.Throwables.EE.Runnable.class, //
            com.landawn.abacus.util.Throwables.EEE.Runnable.class, //
            com.landawn.abacus.util.function.Runnable.class, //
            java.lang.reflect.Type.class }; // "Type" is com.landawn.abacus.type.Type

    @Test
    public void g134_nameOfClassWhoseSimpleNameIsAnotherClassRoundTrips() {
        for (final Class<?> cls : G134_SIMPLE_NAME_IS_ANOTHER_CLASS) {
            final Type<?> type = Type.of(cls);
            assertEquals(cls.getCanonicalName(), type.name(), cls.getName());
            assertEquals(cls.getCanonicalName(), type.xmlName(), cls.getName());
            assertEquals(cls, Type.of(type.name()).javaType(), cls.getName());
            // the pool entry under name() is the very handler of the class
            assertSame(type, Type.of(type.name()), cls.getName());

            final Class<?> arrayClass = java.lang.reflect.Array.newInstance(cls, 0).getClass();
            final Class<?> array2Class = java.lang.reflect.Array.newInstance(arrayClass, 0).getClass();
            assertEquals(cls.getCanonicalName() + "[]", Type.of(arrayClass).name(), cls.getName());
            assertEquals(arrayClass, Type.of(Type.of(arrayClass).name()).javaType(), cls.getName());
            assertEquals(cls.getCanonicalName() + "[][]", Type.of(array2Class).name(), cls.getName());
            assertEquals(array2Class, Type.of(Type.of(array2Class).name()).javaType(), cls.getName());
        }
    }

    @Test
    public void g134_arrayOfJdkClassWhoseSimpleNameIsAnotherClassRoundTrips() {
        // The element handlers have names of their own ("JUDate", "JdkDuration", "JdkOptional<...>"), but the array names
        // are built from the canonical names: "Date[]" is java.sql.Date[], "Duration[]" com.landawn.abacus.util.Duration[],
        // "Optional[]" com.landawn.abacus.util.u.Optional[], "Type[]" com.landawn.abacus.type.Type[].
        final Class<?>[] arrayClasses = { java.util.Date[].class, java.util.Date[][].class, java.time.Duration[].class, java.util.Optional[].class,
                java.util.OptionalInt[].class, java.util.OptionalLong[].class, java.util.OptionalDouble[].class, ProcessBuilder.Redirect.Type[].class };

        for (final Class<?> arrayClass : arrayClasses) {
            final Type<?> type = Type.of(arrayClass);
            assertEquals(arrayClass.getCanonicalName(), type.name(), arrayClass.getName());
            assertEquals(arrayClass, Type.of(type.name()).javaType(), arrayClass.getName());
            assertSame(type, Type.of(type.name()), arrayClass.getName());
        }

        final java.util.Date date = new java.util.Date(86_400_123L);
        final Type<java.util.Date[]> dateArrayType = Type.of(java.util.Date[].class);
        final java.util.Date[] dates = Type.<java.util.Date[]> of(dateArrayType.name()).valueOf(dateArrayType.stringOf(new java.util.Date[] { date }));
        assertEquals(java.util.Date[].class, dates.getClass());
        assertEquals(java.util.Date.class, dates[0].getClass());
        assertEquals(date.getTime(), dates[0].getTime());

        final java.time.Duration[] durations = { java.time.Duration.ofSeconds(5) };
        final Type<java.time.Duration[]> durationArrayType = Type.of(java.time.Duration[].class);
        assertArrayEquals(durations, Type.<java.time.Duration[]> of(durationArrayType.name()).valueOf(durationArrayType.stringOf(durations)));
    }

    @Test
    public void g134_genericTypeWithSuchArgumentRoundTrips() {
        final String listName = "List<com.landawn.abacus.util.function.Runnable>";
        final Type<?> list = Type.of(listName);
        assertEquals(listName, list.name());
        assertEquals(com.landawn.abacus.util.function.Runnable.class, Type.of(list.name()).elementType().javaType());
        assertSame(list, Type.of(list.name()));

        final Type<?> reflected = Type.of(new TypeReference<List<com.landawn.abacus.util.function.Runnable>>() {
        });
        assertEquals(listName, reflected.name());
        assertEquals(com.landawn.abacus.util.function.Runnable.class, Type.of(reflected.name()).elementType().javaType());

        final Type<?> map = Type.ofMap(String.class, Multiset.Entry.class);
        assertEquals("Map<String, com.landawn.abacus.util.Multiset.Entry>", map.name());
        assertEquals(Multiset.Entry.class, Type.of(map.name()).parameterTypes().get(1).javaType());

        final Type<?> dateArrays = Type.ofList(java.util.Date[].class);
        assertEquals("List<java.util.Date[]>", dateArrays.name());
        assertEquals(java.util.Date[].class, Type.of(dateArrays.name()).elementType().javaType());
    }

    @Test
    public void g134_typedXmlAndDatasetJsonReadSuchArraysBackAsTheirOwnClass() {
        final java.util.Date date = new java.util.Date(86_400_123L);
        final java.time.Duration duration = java.time.Duration.ofSeconds(5);
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("dates", new java.util.Date[] { date });
        map.put("durations", new java.time.Duration[] { duration });
        final XmlSerConfig xsc = XmlSerConfig.create().setWriteTypeInfo(true);

        for (final XmlParser parser : new XmlParser[] { ParserFactory.createAbacusXmlParser(), ParserFactory.createXmlParser() }) {
            final String xml = parser.serialize(map, xsc);
            final Map<?, ?> back = parser.deserialize(xml, Map.class);
            // r108 wrote type="Date[]" / "Duration[]" and read java.sql.Date[] / failed on "PT5S"
            assertEquals(java.util.Date[].class, back.get("dates").getClass(), xml);
            assertEquals(java.util.Date.class, ((java.util.Date[]) back.get("dates"))[0].getClass(), xml);
            assertEquals(date.getTime(), ((java.util.Date[]) back.get("dates"))[0].getTime(), xml);
            assertArrayEquals(new java.time.Duration[] { duration }, (java.time.Duration[]) back.get("durations"), xml);
        }

        final JsonSerConfig jsc = JsonSerConfig.create().setWriteColumnType(true);
        final Dataset dataset = N.newDataset(List.of("dates", "durations"),
                List.<List<?>> of(List.of(new java.util.Date[] { date }, new java.time.Duration[] { duration })));
        final String json = ParserFactory.createJsonParser().serialize(dataset, jsc);
        final Dataset back = ParserFactory.createJsonParser().deserialize(json, Dataset.class);
        assertEquals(java.util.Date[].class, back.getColumn("dates").get(0).getClass(), json);
        assertArrayEquals(new java.time.Duration[] { duration }, (java.time.Duration[]) back.getColumn("durations").get(0), json);
    }

    @Test
    public void g134_nameKeepsTheShortFormWhenItResolvesBackToTheSameClass() {
        assertEquals("String", Type.of(String.class).name());
        assertEquals("Integer", Type.of(Integer.class).name());
        assertEquals("int[]", Type.of(int[].class).name());
        assertEquals("String[]", Type.of(String[].class).name());
        assertEquals("Integer[][]", Type.of(Integer[][].class).name());
        assertEquals("List<String>", Type.ofList(String.class).name());
        assertEquals("List<String>", Type.of("java.util.List<java.lang.String>").name());
        assertEquals("Map<String, Integer>", Type.ofMap(String.class, Integer.class).name());
        assertEquals("Map<String, Integer>", Type.of("Map<String, Integer>").name());
        assertEquals("LocalDate", Type.of(java.time.LocalDate.class).name());
        assertEquals("LocalDate", Type.of("java.time.LocalDate").name());
        assertEquals("LocalDate[]", Type.of(java.time.LocalDate[].class).name());
        assertEquals("UUID", Type.of(java.util.UUID.class).name());
        assertEquals("java.util.BitSet", Type.of(java.util.BitSet.class).name());
        assertEquals("Multiset<Object>", Type.of(Multiset.class).name());
        assertEquals("Multiset[]", Type.of(Multiset[].class).name());
        assertEquals("Dataset", Type.of(Dataset.class).name());
        assertEquals("Runnable", Type.of(Runnable.class).name());
        assertEquals("List<Runnable>", Type.of("List<Runnable>").name());
        assertEquals("Iterator", Type.of(java.util.Iterator.class).name());
        assertEquals("Type", Type.of(Type.class).name());
        assertEquals("JUDate", Type.of(java.util.Date.class).name());
        assertEquals("List<JUDate>", Type.ofList(java.util.Date.class).name());
        assertEquals("java.sql.Date[]", Type.of(java.sql.Date[].class).name());
        assertEquals("Map.Entry<Object, Object>", Type.of(java.util.Map.Entry.class).name());

        // A non-public implementation class keeps the short form a reader can construct: Arrays.asList reads back as an ArrayList.
        final Type<?> asListType = Type.of(Arrays.asList(1, 2).getClass());
        assertEquals("ArrayList<Object>", asListType.name());
        assertEquals(ArrayList.class, Type.of(asListType.name()).javaType());

        // Old data written with a short name still reads as the class that name has always resolved to.
        assertEquals(Runnable.class, Type.of("Runnable").javaType());
        assertEquals(java.util.Map.Entry.class, Type.of("Entry").javaType());
        assertEquals(java.sql.Date[].class, Type.of("Date[]").javaType());
        assertEquals(Type.class, Type.of("Type").javaType());
    }

    // ---- deep review 2026-10-27 G134 end ----
}
