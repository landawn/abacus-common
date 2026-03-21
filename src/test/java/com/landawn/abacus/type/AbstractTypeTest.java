package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class AbstractTypeTest extends TestBase {

    private Type<String> stringType;
    private Type<Integer> integerType;
    private Type<List<String>> listType;
    private Type<int[]> intArrayType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        stringType = createType(String.class);
        integerType = createType(Integer.class);
        listType = createType("List<String>");
        intArrayType = createType(int[].class);
    }

    @AfterEach
    public void tearDown() {
    }

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

    // writeCharacter with null value writes NULL_CHAR_ARRAY via AbstractType default implementation
    @Test
    public void testWriteCharacter_NullValue_WritesNullArray() throws IOException {
        BufferedJsonWriter writer = mock(BufferedJsonWriter.class);
        TestSplitType type = new TestSplitType();
        type.writeCharacter(writer, null, null);
        verify(writer, times(1)).write(AbstractType.NULL_CHAR_ARRAY);
    }

    @Test
    public void testGetTypeParameters_ProtectedHelper() {
        TestSplitType helper = new TestSplitType();
        String[] parts = helper.getTypeParametersForTest("Map<String, Integer>");

        assertEquals(2, parts.length);
        assertEquals("String", parts[0]);
        assertEquals("Integer", parts[1]);
    }

    @Test
    public void testGetParameters_ProtectedHelper() {
        TestSplitType helper = new TestSplitType();
        String[] parts = helper.getParametersForTest("HashMap<String, Integer>(16, 0.75f)");

        assertEquals(2, parts.length);
        assertEquals("16", parts[0]);
        assertEquals("0.75f", parts[1]);
    }

    @Test
    @DisplayName("Test AbstractType constructor with simple name")
    public void testConstructorSimpleName() {
        Type<String> type = stringType;
        assertEquals("String", type.name());
        assertEquals("String", type.xmlName());
    }

    @Test
    @DisplayName("Test name()")
    public void testName() {
        assertEquals("String", stringType.name());
    }

    @Test
    @DisplayName("Test declaringName()")
    public void testDeclaringName() {
        assertEquals("List<String>", listType.declaringName());
    }

    @Test
    @DisplayName("Test xmlName()")
    public void testXmlName() {
        assertEquals("String", stringType.xmlName());

        Type<String> genericType = createType("Map<String,Integer>");
        assertEquals("Map&lt;String, Integer&gt;", genericType.xmlName());
    }

    @Test
    @DisplayName("Test isPrimitive()")
    public void testIsPrimitive() {
        assertFalse(stringType.isPrimitive());
    }

    @Test
    @DisplayName("Test isPrimitiveWrapper()")
    public void testIsPrimitiveWrapper() {
        assertFalse(stringType.isPrimitiveWrapper());
    }

    @Test
    @DisplayName("Test isPrimitiveList()")
    public void testIsPrimitiveList() {
        assertFalse(stringType.isPrimitiveList());
    }

    @Test
    @DisplayName("Test isBoolean()")
    public void testIsBoolean() {
        assertFalse(stringType.isBoolean());
    }

    @Test
    @DisplayName("Test isNumber()")
    public void testIsNumber() {
        assertFalse(stringType.isNumber());
    }

    @Test
    @DisplayName("Test isString()")
    public void testIsString() {
        assertFalse(integerType.isString());
    }

    @Test
    @DisplayName("Test isCharSequence()")
    public void testIsCharSequence() {
        assertFalse(integerType.isCharSequence());
    }

    @Test
    @DisplayName("Test isDate()")
    public void testIsDate() {
        assertFalse(stringType.isDate());
    }

    @Test
    @DisplayName("Test isCalendar()")
    public void testIsCalendar() {
        assertFalse(stringType.isCalendar());
    }

    @Test
    @DisplayName("Test isJodaDateTime()")
    public void testIsJodaDateTime() {
        assertFalse(stringType.isJodaDateTime());
    }

    @Test
    @DisplayName("Test isPrimitiveArray()")
    public void testIsPrimitiveArray() {
        assertFalse(stringType.isPrimitiveArray());
    }

    @Test
    @DisplayName("Test isPrimitiveByteArray()")
    public void testIsPrimitiveByteArray() {
        assertFalse(stringType.isPrimitiveByteArray());
    }

    @Test
    @DisplayName("Test isObjectArray()")
    public void testIsObjectArray() {
        assertFalse(stringType.isObjectArray());
    }

    @Test
    @DisplayName("Test isArray()")
    public void testIsArray() {
        assertFalse(stringType.isArray());
    }

    @Test
    @DisplayName("Test isList()")
    public void testIsList() {
        assertFalse(stringType.isList());
    }

    @Test
    @DisplayName("Test isSet()")
    public void testIsSet() {
        assertFalse(stringType.isSet());
    }

    @Test
    @DisplayName("Test isCollection()")
    public void testIsCollection() {
        assertFalse(stringType.isCollection());
    }

    @Test
    @DisplayName("Test isMap()")
    public void testIsMap() {
        assertFalse(stringType.isMap());
    }

    @Test
    @DisplayName("Test isBean()")
    public void testIsBean() {
        assertFalse(stringType.isBean());
    }

    @Test
    @DisplayName("Test isMapEntity()")
    public void testIsMapEntity() {
        assertFalse(stringType.isMapEntity());
    }

    @Test
    @DisplayName("Test isEntityId()")
    public void testIsEntityId() {
        assertFalse(stringType.isEntityId());
    }

    @Test
    @DisplayName("Test isDataset()")
    public void testIsDataset() {
        assertFalse(stringType.isDataset());
    }

    @Test
    @DisplayName("Test isInputStream()")
    public void testIsInputStream() {
        assertFalse(stringType.isInputStream());
    }

    @Test
    @DisplayName("Test isReader()")
    public void testIsReader() {
        assertFalse(stringType.isReader());
    }

    @Test
    @DisplayName("Test isByteBuffer()")
    public void testIsByteBuffer() {
        assertFalse(stringType.isByteBuffer());
    }

    @Test
    @DisplayName("Test isGenericType()")
    public void testIsGenericType() {
        assertFalse(stringType.isParameterizedType());
    }

    @Test
    @DisplayName("Test isImmutable()")
    public void testIsImmutable() {
        assertTrue(stringType.isImmutable());
    }

    @Test
    @DisplayName("Test isComparable()")
    public void testIsComparable() {
        assertTrue(stringType.isComparable());
    }

    // isComparable() default returns false
    @Test
    public void testIsComparable_DefaultReturnsFalse() {
        TestSplitType type = new TestSplitType();
        assertFalse(type.isComparable());
    }

    @Test
    @DisplayName("Test isSerializable()")
    public void testIsSerializable() {
        assertTrue(stringType.isSerializable());
    }

    @Test
    @DisplayName("Test isOptionalOrNullable()")
    public void testIsOptionalOrNullable() {
        assertFalse(stringType.isOptionalOrNullable());
    }

    @Test
    @DisplayName("Test isObject()")
    public void testIsObject() {
        assertFalse(stringType.isObject());
    }

    @Test
    @DisplayName("Test defaultValue()")
    public void testDefaultValue() {
        assertNull(stringType.defaultValue());
    }

    @Test
    @DisplayName("Test isDefaultValue()")
    public void testIsDefaultValue() {
        assertTrue(stringType.isDefaultValue(null));
        assertFalse(stringType.isDefaultValue("test"));
    }

    @Test
    @DisplayName("Test compare() with comparable type")
    public void testCompareComparable() {
        TestComparableType comparableType = new TestComparableType();
        assertEquals(0, comparableType.compare("a", "a"));
        assertTrue(comparableType.compare("a", "b") < 0);
        assertTrue(comparableType.compare("b", "a") > 0);
        assertEquals(0, comparableType.compare(null, null));
        assertTrue(comparableType.compare(null, "a") < 0);
        assertTrue(comparableType.compare("a", null) > 0);
    }

    // compare() throws UnsupportedOperationException when isComparable() returns false (default)
    @Test
    public void testCompare_NonComparable_ThrowsUnsupportedOperation() {
        // stringType uses the default AbstractType which returns false from isComparable()
        // But stringType is configured to be comparable in the test setup. Use a plain type.
        TestSplitType nonComparableType = new TestSplitType();
        assertFalse(nonComparableType.isComparable());
        assertThrows(UnsupportedOperationException.class, () -> nonComparableType.compare("a", "b"));
    }

    @Test
    @DisplayName("Test valueOf(Object)")
    public void testValueOfObject() {
        assertEquals("test", stringType.valueOf((Object) "test"));
        assertNull(stringType.valueOf((Object) null));
    }

    @Test
    @DisplayName("Test valueOf(char[], int, int)")
    public void testValueOfCharArray() {
        char[] chars = "hello world".toCharArray();
        assertEquals("hello", stringType.valueOf(chars, 0, 5));
        assertEquals("world", stringType.valueOf(chars, 6, 5));
        assertNull(stringType.valueOf(null, 0, 0));
    }

    @Test
    @DisplayName("Test getSerializationType()")
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.SERIALIZABLE, stringType.serializationType());
    }

    @Test
    @DisplayName("Test getElementType()")
    public void testGetElementType() {
        assertNull(stringType.elementType());
    }

    @Test
    @DisplayName("Test getParameterTypes()")
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = stringType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(0, paramTypes.length);
    }

    @Test
    @DisplayName("Test get(ResultSet, int)")
    public void testGetResultSetInt() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");

        assertEquals("test", stringType.get(rs, 1));
        verify(rs).getString(1);
    }

    @Test
    @DisplayName("Test get(ResultSet, String)")
    public void testGetResultSetString() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("column")).thenReturn("test");

        assertEquals("test", stringType.get(rs, "column"));
        verify(rs).getString("column");
    }

    @Test
    @DisplayName("Test set(PreparedStatement, int, T)")
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stringType.set(stmt, 1, "test");
        verify(stmt).setString(1, "test");

        stringType.set(stmt, 2, null);
        verify(stmt).setString(2, null);
    }

    @Test
    @DisplayName("Test set(CallableStatement, String, T)")
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        stringType.set(stmt, "param", "test");
        verify(stmt).setString("param", "test");

        stringType.set(stmt, "param2", null);
        verify(stmt).setString("param2", null);
    }

    @Test
    @DisplayName("Test set(PreparedStatement, int, T, int)")
    public void testSetPreparedStatementWithSqlType() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        stringType.set(stmt, 1, "test", java.sql.Types.VARCHAR);
        verify(stmt).setString(1, "test");
    }

    @Test
    @DisplayName("Test set(CallableStatement, String, T, int)")
    public void testSetCallableStatementWithSqlType() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        stringType.set(stmt, "param", "test", java.sql.Types.VARCHAR);
        verify(stmt).setString("param", "test");
    }

    @Test
    @DisplayName("Test appendTo()")
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        stringType.appendTo(sb, "test");
        assertEquals("test", sb.toString());

        sb = new StringBuilder();
        stringType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    @DisplayName("Test writeCharacter() without config")
    public void testWriteCharacterNoConfig() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        stringType.writeCharacter(writer, "test", null);
        verify(writer).writeCharacter("test");

        stringType.writeCharacter(writer, null, null);
        verify(writer).write("null".toCharArray());
    }

    @Test
    @DisplayName("Test writeCharacter() with config")
    public void testWriteCharacterWithConfig() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);

        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        when(config.getStringQuotation()).thenReturn('"');

        stringType.writeCharacter(writer, "test", config);
        verify(writer, times(2)).write('"');
        verify(writer).writeCharacter("test");
    }

    @Test
    @DisplayName("Test collectionToArray()")
    public void testCollection2Array() {
        assertThrows(UnsupportedOperationException.class, () -> {
            stringType.collectionToArray(Arrays.asList("a", "b", "c"));
        });
    }

    @Test
    @DisplayName("Test arrayToCollection(T, Class)")
    public void testArray2CollectionWithClass() {
        assertThrows(UnsupportedOperationException.class, () -> {
            stringType.arrayToCollection("test", ArrayList.class);
        });
    }

    @Test
    @DisplayName("Test arrayToCollection(T, Collection)")
    public void testArray2CollectionWithOutput() {
        assertThrows(UnsupportedOperationException.class, () -> {
            stringType.arrayToCollection("test", new ArrayList<>());
        });
    }

    @Test
    @DisplayName("Test hashCode()")
    public void testHashCode() {
        assertEquals(stringType.name().hashCode(), stringType.hashCode());
    }

    @Test
    @DisplayName("Test hashCode(T)")
    public void testHashCodeValue() {
        assertEquals("test".hashCode(), stringType.hashCode("test"));
        assertEquals(0, stringType.hashCode(null));
    }

    @Test
    @DisplayName("Test deepHashCode(T)")
    public void testDeepHashCode() {
        assertEquals("test".hashCode(), stringType.deepHashCode("test"));
        assertEquals(0, stringType.deepHashCode(null));
    }

    @Test
    public void testEqualsObject() {
        Type<String> anotherStringType = createType(String.class);

        assertTrue(stringType.equals(anotherStringType));
        assertFalse(stringType.equals(integerType));
        assertFalse(stringType.equals("String"));
    }

    @Test
    @DisplayName("Test equals(T, T)")
    public void testEqualsValues() {
        assertTrue(stringType.equals("test", "test"));
        assertFalse(stringType.equals("test1", "test2"));
        assertTrue(stringType.equals(null, null));
        assertFalse(stringType.equals("test", null));
        assertFalse(stringType.equals(null, "test"));
    }

    @Test
    @DisplayName("Test deepEquals(T, T)")
    public void testDeepEquals() {
        assertTrue(stringType.deepEquals("test", "test"));
        assertFalse(stringType.deepEquals("test1", "test2"));
        assertTrue(stringType.deepEquals(null, null));
        assertFalse(stringType.deepEquals("test", null));
    }

    @Test
    @DisplayName("Test toString()")
    public void testToString() {
        assertEquals("String", stringType.toString());
    }

    @Test
    @DisplayName("Test toString(T)")
    public void testToStringValue() {
        assertEquals("test", stringType.toString("test"));
        assertEquals("null", stringType.toString(null));
    }

    @Test
    @DisplayName("Test deepToString(T)")
    public void testDeepToString() {
        assertEquals("test", stringType.deepToString("test"));
        assertEquals("null", stringType.deepToString(null));
    }

    @Test
    public void testSplit_WithSeparator() {
        TestSplitType helper = new TestSplitType();
        // split uses a separator converter internally; comma-separated values
        String[] parts = helper.splitForTest("a,b,c", ",");
        assertNotNull(parts);
        assertEquals(3, parts.length);
        assertEquals("a", parts[0]);
        assertEquals("c", parts[2]);
    }

    @Test
    public void testSplit_WithPipeSeparator() {
        TestSplitType helper = new TestSplitType();
        String[] parts = helper.splitForTest("x|y", "\\|");
        assertNotNull(parts);
        assertEquals(2, parts.length);
    }

    // Cover optimized numeric parsing paths used by primitive type implementations.
    @Test
    public void testParseInt_CharArrayWithSuffixAndOffset() {
        assertEquals(12345, AbstractType.parseInt("__+12345L".toCharArray(), 2, 7));
        assertEquals(-42, AbstractType.parseInt("x-42".toCharArray(), 1, 3));
    }

    // parseInt: length >= 10 falls through to Numbers.toInt
    @Test
    public void testParseInt_LongNumber_FallsBackToNumbersToInt() {
        assertEquals(1234567890, AbstractType.parseInt("1234567890".toCharArray(), 0, 10));
    }

    // parseInt: null or empty char array returns 0
    @Test
    public void testParseInt_NullOrEmpty_ReturnsZero() {
        assertEquals(0, AbstractType.parseInt(null, 0, 0));
        assertEquals(0, AbstractType.parseInt("abc".toCharArray(), 0, 0));
    }

    // parseInt: negative offset/len throws IllegalArgumentException
    @Test
    public void testParseInt_NegativeOffset_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseInt("123".toCharArray(), -1, 3));
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseInt("123".toCharArray(), 0, -1));
    }

    // parseInt: suffix 'L' followed by non-digit throws NumberFormatException
    @Test
    public void testParseInt_SuffixOnlyNoDigit_ThrowsNumberFormat() {
        // "L" alone - suffix consumed leaves len=0, but then cbuf[offset+len-1] check fails
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("L".toCharArray(), 0, 1));
    }

    // parseInt: single non-digit character throws NumberFormatException
    @Test
    public void testParseInt_SingleNonDigit_ThrowsNumberFormat() {
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("x".toCharArray(), 0, 1));
    }

    // parseInt: multi-char with non-digit in middle throws NumberFormatException
    @Test
    public void testParseInt_MultiCharWithNonDigit_ThrowsNumberFormat() {
        assertThrows(NumberFormatException.class, () -> AbstractType.parseInt("12x".toCharArray(), 0, 3));
    }

    // parseLong: null returns 0
    @Test
    public void testParseLong_NullOrEmpty_ReturnsZero() {
        assertEquals(0L, AbstractType.parseLong(null, 0, 0));
        assertEquals(0L, AbstractType.parseLong("abc".toCharArray(), 0, 0));
    }

    @Test
    public void testParseLong_CharArrayWithSuffixAndInvalidLiteral() {
        assertEquals(9876543210123L, AbstractType.parseLong("__9876543210123d".toCharArray(), 2, 14));
        assertThrows(NumberFormatException.class, () -> AbstractType.parseLong("L".toCharArray(), 0, 1));
    }

    // parseLong: negative offset/len throws IllegalArgumentException
    @Test
    public void testParseLong_NegativeOffset_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> AbstractType.parseLong("123".toCharArray(), -1, 3));
    }

    // parseLong: suffix 'f' leaves len=0 returns 0
    @Test
    public void testParseLong_SuffixOnlyNoDigit_ReturnsZeroOrThrows() {
        // "0f" - 'f' suffix removed, len becomes 1, cbuf[0]='0' which is digit
        assertEquals(0L, AbstractType.parseLong("0f".toCharArray(), 0, 2));
    }

}
