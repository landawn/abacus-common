package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Timed;

@Tag("new-test")
public class TimedType100Test extends TestBase {

    private TimedType<String> timedType;

    @BeforeEach
    public void setUp() {
        timedType = (TimedType<String>) createType("Timed<String>");
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(timedType.declaringName());
        assertTrue(timedType.declaringName().contains("Timed"));
    }

    @Test
    public void testClazz() {
        assertEquals(Timed.class, timedType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = timedType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(timedType.isGenericType());
    }

    @Test
    public void testStringOf() {
        Timed<String> timed = Timed.of("test", 123456789L);
        String result = timedType.stringOf(timed);
        assertNotNull(result);
        assertTrue(result.contains("123456789"));
        assertTrue(result.contains("test"));

        assertNull(timedType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        String json = "[123456789, \"test\"]";
        Timed<String> result = timedType.valueOf(json);
        assertNotNull(result);
        assertEquals(123456789L, result.timestamp());
        assertEquals("test", result.value());

        assertNull(timedType.valueOf(null));
        assertNull(timedType.valueOf(""));
        assertThrows(IllegalArgumentException.class, () -> timedType.valueOf(" "));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        Timed<String> timed = Timed.of("test", 123456789L);
        timedType.appendTo(writer, timed);
        String result = writer.toString();
        assertTrue(result.contains("123456789"));
        assertTrue(result.contains("test"));

        writer = new StringWriter();
        timedType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteCharacter() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerializationConfig<?> config = mock(JsonXmlSerializationConfig.class);

        Timed<String> timed = Timed.of("test", 123456789L);
        timedType.writeCharacter(writer, timed, config);

        timedType.writeCharacter(writer, null, config);
    }

    @Test
    public void testGetTypeName() {
        String typeName = TimedType.getTypeName("String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Timed"));
        assertTrue(typeName.contains("String"));

        String declaringName = TimedType.getTypeName("String", true);
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Timed"));
        assertTrue(declaringName.contains("String"));
    }
}
