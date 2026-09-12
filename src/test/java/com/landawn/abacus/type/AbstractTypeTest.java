package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractTypeTest extends TestBase {

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

}
