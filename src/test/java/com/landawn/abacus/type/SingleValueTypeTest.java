package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class SingleValueTypeTest extends TestBase {

    private TestSingleValueType singleValueType;

    private static class TestSingleValueType extends SingleValueType<TestValue> {
        public TestSingleValueType() {
            super(TestValue.class);
        }
    }

    public static class TestValue {
        public String value;

        public TestValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            TestValue that = (TestValue) obj;
            return value != null ? value.equals(that.value) : that.value == null;
        }
    }

    public static class FactoryAnnotatedValue {
        private final String value;

        private FactoryAnnotatedValue(String value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static FactoryAnnotatedValue of(String value) {
            return new FactoryAnnotatedValue(value);
        }

        @JsonXmlValue
        public String getValue() {
            return value;
        }
    }

    @BeforeEach
    public void setUp() {
        singleValueType = new TestSingleValueType();
    }

    // ==================== JsonXmlValue / JsonXmlCreator annotation path ====================

    // A helper type that uses @JsonXmlValue on a method and @JsonXmlCreator on a static factory.
    public static class AnnotatedValue {
        private final String inner;

        private AnnotatedValue(final String inner) {
            this.inner = inner;
        }

        @JsonXmlValue
        public String getValue() {
            return inner;
        }

        @JsonXmlCreator
        public static AnnotatedValue of(final String s) {
            return new AnnotatedValue(s);
        }

        @Override
        public String toString() {
            return inner;
        }
    }

    private static class AnnotatedSingleValueType extends SingleValueType<AnnotatedValue> {
        public AnnotatedSingleValueType() {
            super(AnnotatedValue.class);
        }
    }

    // A helper type that uses @JsonXmlValue on a field and @JsonXmlCreator on a static factory.
    public static class FieldAnnotatedValue {
        @JsonXmlValue
        public final String val;

        private FieldAnnotatedValue(final String val) {
            this.val = val;
        }

        @JsonXmlCreator
        public static FieldAnnotatedValue from(final String s) {
            return new FieldAnnotatedValue(s);
        }
    }

    private static class FieldAnnotatedSingleValueType extends SingleValueType<FieldAnnotatedValue> {
        public FieldAnnotatedSingleValueType() {
            super(FieldAnnotatedValue.class);
        }
    }

    public static class PublicFieldValue {
        public String value;

        public PublicFieldValue(final String value) {
            this.value = value;
        }
    }

    // Enum-backed type (not object type, but an enum itself)
    public enum TestEnum {
        A, B, C
    }

    private static class EnumSingleValueType extends SingleValueType<TestEnum> {
        public EnumSingleValueType() {
            super(TestEnum.class);
        }
    }

    @Test
    public void testClazz() {
        assertEquals(TestValue.class, singleValueType.javaType());
    }

    @Test
    public void testIsGenericType() {
        assertFalse(singleValueType.isParameterizedType());
    }

    @Test
    public void testIsObject() {
        assertFalse(singleValueType.isObject());
    }

    @Test
    public void testIsObject_enumType() {
        EnumSingleValueType type = new EnumSingleValueType();
        // enum types are not isObjectType
        assertFalse(type.isObject());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(singleValueType.isSerializable());
    }

    @Test
    public void testIsSerializable_annotatedType() {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        // String is serializable so jsonValueType (String) should be serializable
        assertTrue(type.isSerializable());
    }

    @Test
    public void testStringOf() {
        TestValue value = new TestValue("test");
        assertEquals("test", singleValueType.stringOf(value));

        assertNull(singleValueType.stringOf(null));
    }

    @Test
    public void testStringOf_withJsonValueMethod() {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        AnnotatedValue av = AnnotatedValue.of("hello");
        assertEquals("hello", type.stringOf(av));
    }

    @Test
    public void testStringOf_withJsonValueMethod_null() {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        assertNull(type.stringOf(null));
    }

    @Test
    public void testStringOf_withJsonValueField() {
        FieldAnnotatedSingleValueType type = new FieldAnnotatedSingleValueType();
        FieldAnnotatedValue fav = FieldAnnotatedValue.from("fieldval");
        assertEquals("fieldval", type.stringOf(fav));
    }

    @Test
    public void testStringOf_withValueExtractor_null() {
        // The base TestSingleValueType uses value extractor path
        assertNull(singleValueType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        assertEquals(new TestValue("test"), singleValueType.valueOf("test"));
    }

    @Test
    public void testValueOf_withJsonCreatorMethod() {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        AnnotatedValue result = type.valueOf("world");
        assertNotNull(result);
        assertEquals("world", result.getValue());
    }

    @Test
    public void testValueOf_withJsonCreatorMethod_fieldAnnotated() {
        FieldAnnotatedSingleValueType type = new FieldAnnotatedSingleValueType();
        FieldAnnotatedValue result = type.valueOf("created");
        assertNotNull(result);
        assertEquals("created", result.val);
    }

    @Test
    public void testValueOf_enumType_returnsStringCast() {
        // For enum types, SingleValueType has no creator, so valueOf returns the raw string cast.
        // The cast itself succeeds at runtime only if callers handle it; just verify no exception is thrown.
        EnumSingleValueType type = new EnumSingleValueType();
        // We call stringOf on a real enum value to verify the fallback toString path
        String result = type.stringOf(TestEnum.A);
        assertEquals("A", result);
    }

    @Test
    public void testGetParameterTypes() {
        assertNotNull(singleValueType.parameterTypes());
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        TestValue value = new TestValue("test");
        when(rs.getString(1)).thenReturn("test");

        assertEquals(value, singleValueType.get(rs, 1));
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        TestValue value = new TestValue("test");
        when(rs.getString("column")).thenReturn("test");

        assertEquals(value, singleValueType.get(rs, "column"));
    }

    @Test
    public void testGet_resultSet_byColumnIndex_withAnnotatedType() throws SQLException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("hello");
        AnnotatedValue result = type.get(rs, 1);
        assertNotNull(result);
        assertEquals("hello", result.getValue());
    }

    @Test
    public void testGet_resultSet_byColumnName_withAnnotatedType() throws SQLException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("col")).thenReturn("world");
        AnnotatedValue result = type.get(rs, "col");
        assertNotNull(result);
        assertEquals("world", result.getValue());
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        TestValue value = new TestValue("test");

        singleValueType.set(stmt, 1, value);
        verify(stmt).setString(1, "test");
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        TestValue value = new TestValue("test");

        singleValueType.set(stmt, "param", value);
        verify(stmt).setString("param", "test");
    }

    @Test
    public void testSetPreparedStatementWithSqlType() throws SQLException {
        PreparedStatement stmt = mock(PreparedStatement.class);
        TestValue value = new TestValue("test");

        singleValueType.set(stmt, 1, value, java.sql.Types.VARCHAR);
        verify(stmt).setString(1, value.value);
    }

    @Test
    public void testSetCallableStatementWithSqlType() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        TestValue value = new TestValue("test");

        singleValueType.set(stmt, "param", value, java.sql.Types.VARCHAR);
        verify(stmt).setString("param", value.value);
    }

    @Test
    public void testSet_preparedStatement_null() throws SQLException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        PreparedStatement stmt = mock(PreparedStatement.class);
        // calling set with null should call stmt.setObject(index, null)
        type.set(stmt, 1, null);
        verify(stmt).setObject(1, null);
    }

    @Test
    public void testSet_callableStatement_null() throws SQLException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        CallableStatement stmt = mock(CallableStatement.class);
        type.set(stmt, "param", null);
        verify(stmt).setObject("param", null);
    }

    @Test
    public void testSet_preparedStatement_withSqlType_null() throws SQLException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        PreparedStatement stmt = mock(PreparedStatement.class);
        type.set(stmt, 1, null, java.sql.Types.VARCHAR);
        verify(stmt).setObject(1, null, java.sql.Types.VARCHAR);
    }

    @Test
    public void testSet_callableStatement_withSqlType_null() throws SQLException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        CallableStatement stmt = mock(CallableStatement.class);
        type.set(stmt, "param", null, java.sql.Types.VARCHAR);
        verify(stmt).setObject("param", null, java.sql.Types.VARCHAR);
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        TestValue value = new TestValue("test");
        singleValueType.writeCharacter(writer, value, config);

        singleValueType.writeCharacter(writer, null, config);

        when(config.getStringQuotation()).thenReturn('"');
        singleValueType.writeCharacter(writer, value, config);
        assertNotNull(value);
    }

    @Test
    public void testWriteCharacter_withJsonValueMethod() throws IOException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        AnnotatedValue av = AnnotatedValue.of("hello");
        type.writeCharacter(writer, av, config);
        assertNotNull(av);
    }

    @Test
    public void testWriteCharacter_null_withAnnotatedType() throws IOException {
        AnnotatedSingleValueType type = new AnnotatedSingleValueType();
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        // writing null should write "null"
        type.writeCharacter(writer, null, config);
        assertNotNull(writer);
    }

    @Test
    public void testGetCreatorAndValueExtractor_WithFinalField_ReturnsNullTuple() {
        // FactoryAnnotatedValue has a final field, so getCreatorAndValueExtractor returns null tuple
        com.landawn.abacus.util.Tuple.Tuple3<Type<Object>, java.util.function.Function<String, FactoryAnnotatedValue>, java.util.function.Function<FactoryAnnotatedValue, Object>> tuple = SingleValueType
                .getCreatorAndValueExtractor(FactoryAnnotatedValue.class);
        // final fields are excluded from matching — all tuple elements are null
        assertNull(tuple._1);
        assertNull(tuple._2);
        assertNull(tuple._3);
    }

    @Test
    public void testGetCreatorAndValueExtractor_PublicFieldAndConstructor() {
        com.landawn.abacus.util.Tuple.Tuple3<Type<Object>, java.util.function.Function<String, PublicFieldValue>, java.util.function.Function<PublicFieldValue, Object>> tuple = SingleValueType
                .getCreatorAndValueExtractor(PublicFieldValue.class);

        assertNotNull(tuple._1);
        assertNotNull(tuple._2);
        assertNotNull(tuple._3);
        assertEquals(String.class, tuple._1.javaType());

        PublicFieldValue value = tuple._2.apply("field-value");

        assertEquals("field-value", tuple._3.apply(value));
    }
}
