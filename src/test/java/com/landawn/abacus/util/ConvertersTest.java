package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ConvertersTest extends TestBase {

    @Test
    public void testRegister_nullArguments() {
        final BiFunction<Object, Class<?>, String> converter = (value, targetType) -> value == null ? null : value.toString();

        assertThrows(IllegalArgumentException.class, () -> Converters.register(null, converter));

        // The null-converter check fires before the built-in class check.
        assertThrows(IllegalArgumentException.class, () -> Converters.register(String.class, null));
    }

    @Test
    public void testConvert_usesSuperclassConverter() {
        CommonUtil.registerConverter(testfixtures.ConvertHierarchy.Base.class, (value, targetType) -> "from-base");

        assertEquals("from-base", CommonUtil.convert(new testfixtures.ConvertHierarchy.Child(), String.class));
        assertEquals("from-base", CommonUtil.convert(new testfixtures.ConvertHierarchy.Base(), String.class));
    }
}
