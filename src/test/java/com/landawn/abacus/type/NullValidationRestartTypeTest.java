package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.sql.ResultSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NullValidationRestartTypeTest extends TestBase {
    @Test
    public void typeConstructorsRejectMissingNamesAndClassTokens() {
        assertThrowsExactly(IllegalArgumentException.class, () -> new ObjectType<>((Class<Object>) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new ObjectType<>(null, Object.class));
        assertThrowsExactly(IllegalArgumentException.class, () -> new ObjectType<>("Custom", (Class<Object>) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new NumberType<>((String) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new NumberType<>((Class<?>) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new NumberType<>("Custom", null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new CalendarType(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new DateType(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new BeanType<>(null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new CollectionType<>(null, "String"));
        assertThrowsExactly(IllegalArgumentException.class, () -> new MapType<>(null, "String", "Integer"));
        assertThrowsExactly(IllegalArgumentException.class, () -> new ImmutableMapType<>(null, "String", "Integer"));
        assertThrowsExactly(IllegalArgumentException.class, () -> new ImmutableSetType<>(null, "String"));
        assertThrowsExactly(IllegalArgumentException.class, () -> new ObjectArrayType<>((Class<Object[]>) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> new ObjectArrayType<>((Type<Object>) null));
    }

    @Test
    public void builtInTypesPreserveNullValuesAndLowLevelNpeContracts() {
        assertEquals(Integer.valueOf(123), Type.of(Integer.class).valueOf("123"));
        assertEquals("hello", Type.of(String.class).valueOf("hello"));
        assertNull(Type.of(String.class).stringOf(null));
        assertThrowsExactly(NullPointerException.class, () -> Type.of(Integer.class).get((ResultSet) null, 1));
        assertThrowsExactly(NullPointerException.class, () -> Type.of(String.class).appendTo(null, "value"));
        assertEquals(Object.class, new ObjectType<>(Object.class).javaType());
        assertEquals(String[].class, new ObjectArrayType<>(Type.of(String.class)).javaType());
    }
}
