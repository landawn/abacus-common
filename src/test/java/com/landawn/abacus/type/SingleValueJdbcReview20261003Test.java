package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SingleValueJdbcReview20261003Test extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void factoryReceivesTheNativeBinaryValue(final boolean byName) throws Exception {
        final byte[] expected = { 0, 1, -1, 37 };
        final Type<BytesValue> type = Type.of(BytesValue.class);
        assertTrue(type.isSerializable());
        try (CachedRowSet row = row(Types.VARBINARY, expected)) {
            assertArrayEquals(expected, get(type, row, byName).value());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void constructorReceivesTheDeclaredBinaryValue(final boolean byName) throws Exception {
        final byte[] expected = { 12, -8, 33 };
        final Type<BytesConstructor> type = Type.of(BytesConstructor.class);
        assertTrue(type.isSerializable());
        try (CachedRowSet row = row(Types.VARBINARY, expected)) {
            assertArrayEquals(expected, get(type, row, byName).value());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void binaryReadDoesNotRequireDriverStringConversion(final boolean byName) throws Exception {
        final ResultSet row = mock(ResultSet.class);
        final byte[] expected = { 1, 2, 3 };
        when(row.getBytes(1)).thenReturn(expected);
        when(row.getBytes("v")).thenReturn(expected);
        when(row.getString(1)).thenThrow(new SQLFeatureNotSupportedException("no text conversion"));
        when(row.getString("v")).thenThrow(new SQLFeatureNotSupportedException("no text conversion"));
        assertArrayEquals(expected, get(Type.of(BytesValue.class), row, byName).value());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void sqlNullIsDistinctFromPrimitiveZero(final boolean byName) throws Exception {
        try (CachedRowSet row = row(Types.INTEGER, null)) {
            assertNull(get(Type.of(IntValue.class), row, byName));
        }
        try (CachedRowSet row = row(Types.INTEGER, 0)) {
            assertEquals(0, get(Type.of(IntValue.class), row, byName).value());
        }
        try (CachedRowSet row = row(Types.VARBINARY, null)) {
            assertNull(get(Type.of(BytesValue.class), row, byName));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void timestampNanosecondsAreNotCanonicalizedThroughText(final boolean byName) throws Exception {
        final Timestamp expected = Timestamp.valueOf("2024-05-06 07:08:09.123456789");
        try (CachedRowSet row = row(Types.TIMESTAMP, expected)) {
            assertEquals(expected, get(Type.of(StampValue.class), row, byName).value());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void stringAndOrdinaryTextCreatorsKeepTheirContracts(final boolean byName) throws Exception {
        try (CachedRowSet row = row(Types.VARCHAR, "null")) {
            assertEquals("null", get(Type.of(TextValue.class), row, byName).value());
        }
        assertArrayEquals(new byte[] { 1, 2 }, Type.of(BytesValue.class).valueOf("[1, 2]").value());
        assertArrayEquals(new byte[] { 3, 4 }, Type.of(BytesConstructor.class).valueOf("[3, 4]").value());
        assertEquals("ordinary", SingleValueType.getCreatorAndValueExtractor(TextValue.class)._2.apply("ordinary").value());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void resolvedGenericValueTypeIsRetained(final boolean byName) throws Exception {
        final Type<GenericValue<List<BigDecimal>>> type = Type.of(GenericValue.class.getName() + "<List<BigDecimal>>");
        try (CachedRowSet row = row(Types.VARCHAR, "[1.2300, 12345678901234567890.0123456789]")) {
            assertEquals(List.of(new BigDecimal("1.2300"), new BigDecimal("12345678901234567890.0123456789")), get(type, row, byName).value());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void creatorAndJdbcExceptionsRemainUnwrapped(final boolean byName) throws Exception {
        try (CachedRowSet row = row(Types.INTEGER, -1)) {
            assertSame(IntValue.FAILURE, assertThrows(IllegalArgumentException.class, () -> get(Type.of(IntValue.class), row, byName)));
        }
        final ResultSet row = mock(ResultSet.class);
        final SQLException failure = new SQLException("closed");
        when(row.getInt(1)).thenThrow(failure);
        when(row.getInt("v")).thenThrow(failure);
        when(row.getString(1)).thenThrow(failure);
        when(row.getString("v")).thenThrow(failure);
        assertSame(failure, assertThrows(SQLException.class, () -> get(Type.of(IntValue.class), row, byName)));
    }

    private static <T> T get(final Type<T> type, final ResultSet row, final boolean byName) throws SQLException {
        return byName ? type.get(row, "v") : type.get(row, 1);
    }

    private static CachedRowSet row(final int sqlType, final Object value) throws SQLException {
        final CachedRowSet row = RowSetProvider.newFactory().createCachedRowSet();
        final RowSetMetaDataImpl metadata = new RowSetMetaDataImpl();
        metadata.setColumnCount(1);
        metadata.setColumnName(1, "v");
        metadata.setColumnLabel(1, "v");
        metadata.setColumnType(1, sqlType);
        row.setMetaData(metadata);
        row.moveToInsertRow();
        row.updateObject(1, value);
        row.insertRow();
        row.moveToCurrentRow();
        row.beforeFirst();
        row.next();
        return row;
    }

    public static class BytesValue {
        private byte[] value;

        private BytesValue(final byte[] value) {
            this.value = value;
        }

        public static BytesValue of(final byte[] value) {
            return new BytesValue(value);
        }

        public byte[] value() {
            return value;
        }
    }

    public static class BytesConstructor {
        private byte[] value;

        public BytesConstructor(final Object value) {
            this.value = (byte[]) value;
        }

        public byte[] value() {
            return value;
        }
    }

    public static class IntValue {
        static final IllegalArgumentException FAILURE = new IllegalArgumentException("negative");
        private int value;

        private IntValue(final int value) {
            this.value = value;
        }

        public static IntValue of(final int value) {
            if (value < 0) {
                throw FAILURE;
            }
            return new IntValue(value);
        }

        public int value() {
            return value;
        }
    }

    public static class StampValue {
        private Timestamp value;

        private StampValue(final Timestamp value) {
            this.value = value;
        }

        public static StampValue of(final Timestamp value) {
            return new StampValue(value);
        }

        public Timestamp value() {
            return value;
        }
    }

    public static class TextValue {
        private String value;

        private TextValue(final String value) {
            this.value = value;
        }

        public static TextValue of(final String value) {
            return new TextValue(value);
        }

        public String value() {
            return value;
        }
    }

    public static class GenericValue<T> {
        private T value;

        private GenericValue(final T value) {
            this.value = value;
        }

        public static <T> GenericValue<T> of(final T value) {
            return new GenericValue<>(value);
        }

        public T value() {
            return value;
        }
    }
}
