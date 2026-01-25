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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class SingleValueType100Test extends TestBase {

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

    @BeforeEach
    public void setUp() {
        singleValueType = new TestSingleValueType();
    }

    @Test
    public void testClazz() {
        assertEquals(TestValue.class, singleValueType.clazz());
    }

    @Test
    public void testIsGenericType() {
        assertFalse(singleValueType.isGenericType());
    }

    @Test
    public void testGetParameterTypes() {
        assertNotNull(singleValueType.getParameterTypes());
    }

    @Test
    public void testIsObjectType() {
        assertFalse(singleValueType.isObjectType());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(singleValueType.isSerializable());
    }

    @Test
    public void testStringOf() {
        TestValue value = new TestValue("test");
        assertEquals("test", singleValueType.stringOf(value));

        assertNull(singleValueType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        assertEquals(new TestValue("test"), singleValueType.valueOf("test"));
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
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        TestValue value = new TestValue("test");
        singleValueType.writeCharacter(writer, value, config);

        singleValueType.writeCharacter(writer, null, config);

        when(config.getStringQuotation()).thenReturn('"');
        singleValueType.writeCharacter(writer, value, config);
    }
}
