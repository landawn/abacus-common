package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.sql.Array;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLXML;
import java.sql.Struct;
import java.util.stream.Stream;

import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialRef;
import javax.sql.rowset.serial.SerialStruct;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class TypeFactoryJdbcTypeTest extends TestBase {

    static Stream<Arguments> jdbcTypes() {
        return Stream.of(Arguments.of(Array.class, SQLArrayType.class, "SQLArray"), Arguments.of(Ref.class, RefType.class, "Ref"),
                Arguments.of(RowId.class, RowIdType.class, "RowId"), Arguments.of(SQLXML.class, SQLXMLType.class, "SQLXML"),
                Arguments.of(Struct.class, StructType.class, "Struct"));
    }

    @ParameterizedTest
    @MethodSource("jdbcTypes")
    public void testInterfaceAndImplementationLookups(final Class<?> jdbcClass, final Class<?> handlerClass, final String typeName) {
        final Type<?> interfaceType = TypeFactory.getType(jdbcClass);
        assertEquals(handlerClass, interfaceType.getClass());
        assertEquals(jdbcClass, interfaceType.javaType());
        assertSame(interfaceType, TypeFactory.getType(typeName));
        assertSame(interfaceType, TypeFactory.getType(jdbcClass.getName()));

        // Different proxy classes exercise both cold class-first and cold name-first lookups.
        for (final boolean nameFirst : new boolean[] { false, true }) {
            final Class<?>[] interfaces = nameFirst ? new Class<?>[] { jdbcClass, Serializable.class } : new Class<?>[] { jdbcClass };
            final Object value = Proxy.newProxyInstance(getClass().getClassLoader(), interfaces, (proxy, method, args) -> {
                throw new AssertionError("JDBC value must not be accessed: " + method.getName());
            });
            final Class<?> implementationClass = value.getClass();
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.registerType(implementationClass, valueToSerialize -> "", text -> null));
            final Type<Object> type = nameFirst ? TypeFactory.getType(implementationClass.getName()) : TypeFactory.getType(implementationClass);

            assertEquals(handlerClass, type.getClass());
            assertNotSame(interfaceType, type);
            assertEquals(implementationClass, type.javaType());
            assertEquals(implementationClass, type.reflectType());
            assertEquals(typeName, type.name());
            assertFalse(type.isSerializable());
            assertSame(type, TypeFactory.getType(implementationClass));
            assertSame(type, TypeFactory.getType(implementationClass.getName()));
            assertSame(type, TypeFactory.getType((java.lang.reflect.Type) implementationClass));
            assertSame(value, type.valueOf(value));
            assertNull(type.valueOf((Object) null));
            assertThrows(UnsupportedOperationException.class, () -> type.valueOf("value"));
            assertThrows(UnsupportedOperationException.class, () -> type.valueOf((String) null));
            assertThrows(UnsupportedOperationException.class, () -> type.valueOf(new Object() {
                @Override
                public String toString() {
                    throw new AssertionError("Foreign values must not be serialized");
                }
            }));
            assertNull(type.stringOf(null));
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(implementationClass.getName() + "(unexpected)"));
        }

        // Implementation lookups must not replace the shared interface aliases.
        assertSame(interfaceType, TypeFactory.getType(jdbcClass));
        assertSame(interfaceType, TypeFactory.getType(typeName));
        assertSame(interfaceType, TypeFactory.getType(jdbcClass.getName()));
    }

    @ParameterizedTest
    @MethodSource("jdbcTypes")
    public void testNamedRegistrationDoesNotReplaceJdbcHandler(final Class<?> jdbcClass, final Class<?> handlerClass, final String typeName) {
        final Object value = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { jdbcClass, Runnable.class }, (proxy, method, args) -> {
            throw new AssertionError("JDBC value must not be accessed: " + method.getName());
        });
        final Class<?> implementationClass = value.getClass();
        final String alias = "JdbcCustom" + typeName + System.nanoTime();

        // Register the alternate name before this implementation class has ever been looked up.
        TypeFactory.registerType(alias, implementationClass, ignored -> "custom", text -> null);
        final Type<?> namedType = TypeFactory.getType(alias);
        assertEquals("custom", namedType.stringOf(null));
        assertEquals(implementationClass, namedType.javaType());

        final Type<?> jdbcType = TypeFactory.getType(implementationClass);
        assertEquals(handlerClass, jdbcType.getClass());
        assertEquals(implementationClass, jdbcType.javaType());
        assertEquals(typeName, jdbcType.name());
        assertNotSame(namedType, jdbcType);
        assertSame(jdbcType, TypeFactory.getType(implementationClass.getName()));
        assertSame(value, jdbcType.valueOf(value));
    }

    static Stream<Arguments> serialJdbcTypes() {
        return Stream.of(Arguments.of(SerialArray.class, SQLArrayType.class), Arguments.of(SerialRef.class, RefType.class),
                Arguments.of(SerialStruct.class, StructType.class));
    }

    @ParameterizedTest
    @MethodSource("serialJdbcTypes")
    public void testStandardImplementationLookups(final Class<?> implementationClass, final Class<?> handlerClass) {
        final Type<?> type = TypeFactory.getType(implementationClass);
        assertEquals(handlerClass, type.getClass());
        assertEquals(implementationClass, type.javaType());
        assertFalse(type.isSerializable());
    }
}
