package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Duration;

@Tag("new-test")
public class DurationType100Test extends TestBase {

    private DurationType durationType;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        durationType = (DurationType) createType(Duration.class.getSimpleName());
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testClazz() {
        assertEquals(Duration.class, durationType.clazz());
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(durationType.isNonQuotableCsvType());
    }

    @Test
    public void testStringOf() {
        assertNull(durationType.stringOf(null));

        Duration duration = Duration.ofMillis(1000);
        assertEquals("1000", durationType.stringOf(duration));

        Duration longDuration = Duration.ofMillis(123456789);
        assertEquals("123456789", durationType.stringOf(longDuration));
    }

    @Test
    public void testValueOf() {
        assertNull(durationType.valueOf(null));
        assertNull(durationType.valueOf(""));

        Duration duration = durationType.valueOf("1000");
        assertNotNull(duration);
        assertEquals(1000, duration.toMillis());

        duration = durationType.valueOf("123456789");
        assertNotNull(duration);
        assertEquals(123456789, duration.toMillis());
    }

    @Test
    public void testGetByColumnIndex() throws SQLException {
        when(resultSet.getLong(1)).thenReturn(1000L);

        Duration result = durationType.get(resultSet, 1);
        assertNotNull(result);
        assertEquals(1000, result.toMillis());
        verify(resultSet).getLong(1);
    }

    @Test
    public void testGetByColumnLabel() throws SQLException {
        when(resultSet.getLong("durationColumn")).thenReturn(5000L);

        Duration result = durationType.get(resultSet, "durationColumn");
        assertNotNull(result);
        assertEquals(5000, result.toMillis());
        verify(resultSet).getLong("durationColumn");
    }

    @Test
    public void testSetPreparedStatement() throws SQLException {
        Duration duration = Duration.ofMillis(1000);
        durationType.set(preparedStatement, 1, duration);
        verify(preparedStatement).setLong(1, 1000);

        durationType.set(preparedStatement, 2, null);
        verify(preparedStatement).setLong(2, 0);
    }

    @Test
    public void testSetCallableStatement() throws SQLException {
        Duration duration = Duration.ofMillis(2000);
        durationType.set(callableStatement, "durationParam", duration);
        verify(callableStatement).setLong("durationParam", 2000);

        durationType.set(callableStatement, "nullParam", null);
        verify(callableStatement).setLong("nullParam", 0);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        durationType.appendTo(writer, null);
        assertEquals("null", writer.toString());

        writer = new StringWriter();
        Duration duration = Duration.ofMillis(1234);
        durationType.appendTo(writer, duration);
        assertEquals("1234", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        durationType.writeCharacter(characterWriter, null, null);

        Duration duration = Duration.ofMillis(5678);
        durationType.writeCharacter(characterWriter, duration, null);
    }
}
