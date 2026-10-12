package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;

/**
 * Pins the supported JDBC storage of enums: a {@code String} (name or annotated value) in a character column, or an
 * {@code int} (ordinal, code or annotated value) in an integer column. A creator-less annotated enum is read through
 * {@code getString}, which yields exactly the annotated value's text for both storage forms.
 */
@Tag("unit")
class EnumJdbcStringOrIntStorageTest extends TestBase {

    @Test
    void annotatedStringValueRoundTripsThroughACharacterColumn() throws Exception {
        final Type<TextValue> type = Type.of(TextValue.class);

        final PreparedStatement stmt = mock(PreparedStatement.class);
        type.set(stmt, 1, TextValue.ALPHA);
        verify(stmt).setString(1, "a1");

        try (CachedRowSet row = row(Types.VARCHAR, "a1")) {
            assertEquals(TextValue.ALPHA, type.get(row, 1));
            assertEquals(TextValue.ALPHA, type.get(row, "v"));
        }
    }

    @Test
    void annotatedStringEnumAlsoAcceptsConstantNamesAndAliases() throws Exception {
        final Type<TextValue> type = Type.of(TextValue.class);

        try (CachedRowSet row = row(Types.VARCHAR, "ALPHA")) {
            assertEquals(TextValue.ALPHA, type.get(row, 1));
        }
        try (CachedRowSet row = row(Types.VARCHAR, "alias")) {
            assertEquals(TextValue.ALPHA, type.get(row, "v"));
        }
    }

    @Test
    void annotatedIntValueRoundTripsThroughAnIntegerColumn() throws Exception {
        final Type<IntValue> type = Type.of(IntValue.class);

        final PreparedStatement stmt = mock(PreparedStatement.class);
        type.set(stmt, 1, IntValue.SEVEN);
        verify(stmt).setInt(1, 7);

        try (CachedRowSet row = row(Types.INTEGER, 7)) {
            assertEquals(IntValue.SEVEN, type.get(row, 1));
            assertEquals(IntValue.SEVEN, type.get(row, "v"));
        }
        try (CachedRowSet row = row(Types.INTEGER, 0)) {
            assertEquals(IntValue.ZERO, type.get(row, 1));
        }
    }

    @Test
    void sqlNullAndLiteralNullReadAsNull() throws Exception {
        try (CachedRowSet row = row(Types.VARCHAR, null)) {
            assertNull(Type.of(TextValue.class).get(row, 1));
            assertNull(Type.of(TextValue.class).get(row, "v"));
        }
        try (CachedRowSet row = row(Types.INTEGER, null)) {
            assertNull(Type.of(IntValue.class).get(row, 1));
            assertNull(Type.of(IntValue.class).get(row, "v"));
        }
        try (CachedRowSet row = row(Types.VARCHAR, "null")) {
            assertNull(Type.of(TextValue.class).get(row, 1));
        }
    }

    @Test
    void unknownStoredValueIsRejected() throws Exception {
        try (CachedRowSet row = row(Types.INTEGER, 99)) {
            assertThrows(IllegalArgumentException.class, () -> Type.of(IntValue.class).get(row, 1));
        }
        try (CachedRowSet row = row(Types.VARCHAR, "zz")) {
            assertThrows(IllegalArgumentException.class, () -> Type.of(TextValue.class).get(row, "v"));
        }
    }

    @Test
    void nameOrdinalAndCodeRepresentationsRoundTrip() throws Exception {
        final PreparedStatement stmt = mock(PreparedStatement.class);
        Type.of(Plain.class).set(stmt, 1, Plain.SECOND);
        verify(stmt).setString(1, "SECOND");

        try (CachedRowSet row = row(Types.VARCHAR, "SECOND")) {
            assertEquals(Plain.SECOND, Type.of(Plain.class).get(row, 1));
        }
        try (CachedRowSet row = row(Types.INTEGER, 1)) {
            assertEquals(Plain.SECOND, Type.of(Plain.class.getName() + "(ORDINAL)").get(row, "v"));
        }
        try (CachedRowSet row = row(Types.INTEGER, 20)) {
            assertEquals(Plain.SECOND, Type.of(Plain.class.getName() + "(CODE)").get(row, 1));
        }
        // Ordinal and code are read as numbers, so a scaled numeric column is fine for them.
        try (CachedRowSet row = row(Types.DECIMAL, new BigDecimal("1.00"))) {
            assertEquals(Plain.SECOND, Type.of(Plain.class.getName() + "(ORDINAL)").get(row, 1));
        }
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

    public enum TextValue {
        @JsonXmlField(name = "alias")
        ALPHA;

        @JsonValue
        public String value() {
            return "a1";
        }
    }

    public enum IntValue {
        ZERO, SEVEN;

        @JsonValue
        public int value() {
            return this == ZERO ? 0 : 7;
        }
    }

    public enum Plain {
        FIRST, SECOND;

        public int code() {
            return this == FIRST ? 10 : 20;
        }
    }
}
