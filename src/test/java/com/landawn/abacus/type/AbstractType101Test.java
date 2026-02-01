package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.type.Type.SerializationType;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class AbstractType101Test extends TestBase {

    private Type<String> testType;
    private CharacterWriter writer;
    private JsonXmlSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        testType = createType("String");
        writer = createCharacterWriter();
        config = mock(JsonXmlSerializationConfig.class);
    }

    @Test
    public void testName() {
        assertEquals("String", testType.name());
    }

    @Test
    public void testDeclaringName() {
        assertEquals("String", testType.declaringName());
    }

    @Test
    public void testXmlName() {
        Type<String> genericType = createType("Map<String, Integer>");
        assertEquals("Map&lt;String, Integer&gt;", genericType.xmlName());
    }

    @Test
    public void testDefaultBooleanMethods() {
        assertFalse(testType.isPrimitiveType());
        assertFalse(testType.isPrimitiveWrapper());
        assertFalse(testType.isPrimitiveList());
        assertFalse(testType.isBoolean());
        assertFalse(testType.isNumber());
        assertTrue(testType.isString());
        assertTrue(testType.isCharSequence());
        assertFalse(testType.isDate());
        assertFalse(testType.isCalendar());
        assertFalse(testType.isJodaDateTime());
        assertFalse(testType.isPrimitiveArray());
        assertFalse(testType.isPrimitiveByteArray());
        assertFalse(testType.isObjectArray());
        assertFalse(testType.isArray());
        assertFalse(testType.isList());
        assertFalse(testType.isSet());
        assertFalse(testType.isCollection());
        assertFalse(testType.isMap());
        assertFalse(testType.isBean());
        assertFalse(testType.isMapEntity());
        assertFalse(testType.isEntityId());
        assertFalse(testType.isDataset());
        assertFalse(testType.isInputStream());
        assertFalse(testType.isReader());
        assertFalse(testType.isByteBuffer());
        assertFalse(testType.isParameterizedType());
        assertTrue(testType.isImmutable());
        assertTrue(testType.isComparable());
        assertTrue(testType.isSerializable());
        assertFalse(testType.isOptionalOrNullable());
        assertFalse(testType.isObjectType());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(SerializationType.SERIALIZABLE, testType.getSerializationType());
    }

    @Test
    public void testGetElementType() {
        assertNull(testType.getElementType());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = testType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(0, paramTypes.length);
    }

    @Test
    public void testDefaultValue() {
        assertNull(testType.defaultValue());
    }

    @Test
    public void testIsDefaultValue() {
        assertTrue(testType.isDefaultValue(null));
        assertFalse(testType.isDefaultValue("test"));
    }

    @Test
    public void testCompare() {

        assertEquals(-1, testType.compare("a", "b"));
        Type<Integer> intType = createType("Integer");
        assertEquals(0, intType.compare(null, null));
        assertEquals(-1, intType.compare(null, 5));
        assertEquals(1, intType.compare(5, null));
        assertEquals(-1, intType.compare(3, 5));
        assertEquals(0, intType.compare(5, 5));
        assertEquals(1, intType.compare(7, 5));
    }

    @Test
    public void testValueOfObject() {
        assertEquals("123", testType.valueOf(123));
        assertEquals("true", testType.valueOf(true));
        assertNull(testType.valueOf((Object) null));
    }

    @Test
    public void testValueOfCharArray() {
        char[] chars = "hello".toCharArray();
        assertEquals("hello", testType.valueOf(chars, 0, 5));
        assertEquals("ell", testType.valueOf(chars, 1, 3));
        assertNull(testType.valueOf(null, 0, 0));
    }

    @Test
    public void testGetFromResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test1");
        when(rs.getString("col1")).thenReturn("test2");

        assertEquals("test1", testType.get(rs, 1));
        assertEquals("test2", testType.get(rs, "col1"));
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        testType.set(stmt, 1, "value");
        verify(stmt).setString(1, "value");
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        testType.set(stmt, "param", "value");
        verify(stmt).setString("param", "value");
    }

    @Test
    public void testSetWithSqlType() throws SQLException {
        PreparedStatement pstmt = mock(PreparedStatement.class);
        CallableStatement cstmt = mock(CallableStatement.class);

        testType.set(pstmt, 1, "value", 100);
        verify(pstmt).setString(1, "value");

        testType.set(cstmt, "param", "value", 100);
        verify(cstmt).setString("param", "value");
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();

        testType.appendTo(sb, null);
        assertEquals("null", sb.toString());

        sb.setLength(0);
        testType.appendTo(sb, "test");
        assertEquals("test", sb.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        testType.writeCharacter(writer, null, null);
        verify(writer).write(any(char[].class));

        testType.writeCharacter(writer, "test", null);
        verify(writer).writeCharacter("test");

        when(config.getStringQuotation()).thenReturn('\'');
        testType.writeCharacter(writer, "test", config);
        verify(writer, times(2)).write('\'');
        verify(writer, times(2)).writeCharacter("test");
    }

    @Test
    public void testUnsupportedOperations() {
        assertThrows(UnsupportedOperationException.class, () -> testType.collectionToArray(new ArrayList<>()));
        assertThrows(UnsupportedOperationException.class, () -> testType.arrayToCollection("test", List.class));
        assertThrows(UnsupportedOperationException.class, () -> testType.arrayToCollection("test", new ArrayList<>()));
    }

    @Test
    public void testHashCode() {
        assertEquals(testType.name().hashCode(), testType.hashCode());
    }

    @Test
    public void testDeepHashCode() {
        assertEquals(N.hashCode("test"), testType.deepHashCode("test"));
        assertEquals(N.hashCode((String) null), testType.deepHashCode((String) null));
    }

    @Test
    public void testEquals() {
        assertTrue(testType.equals(testType));
        assertFalse(testType.equals(null));
        assertFalse(testType.equals("not a type"));

        Type<String> sameType = createType("String");
        assertTrue(testType.equals(sameType));

        Type<String> differentType = createType("DifferentType");
        assertFalse(testType.equals(differentType));
    }

    @Test
    public void testDeepEquals() {
        assertTrue(testType.deepEquals("test", "test"));
        assertFalse(testType.deepEquals("test", "different"));
        assertTrue(testType.deepEquals(null, null));
        assertFalse(testType.deepEquals("test", null));
    }

    @Test
    public void testToString() {
        assertEquals("String", testType.toString());
        assertEquals("test", testType.toString("test"));
        assertEquals("null", testType.toString(null));
    }

    @Test
    public void testDeepToString() {
        assertEquals("test", testType.deepToString("test"));
        assertEquals("null", testType.deepToString(null));
    }

}
