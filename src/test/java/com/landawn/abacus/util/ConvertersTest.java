package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;

import com.landawn.abacus.TestBase;

@Isolated("Mutates the process-global converter registry (testGetConverter_emptyAndPopulatedRegistry clears and restores it)")
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

    // ---- deep review 2026-11-02 G034 begin ----
    // G034-01: pin getConverter on an empty registry (early return) and on a populated one (exact class, superclass walk,
    // Object/unrelated/null -> null). The process-global registry is saved, cleared and restored around the test.
    @Test
    @SuppressWarnings("unchecked")
    public void testGetConverter_emptyAndPopulatedRegistry() throws Exception {
        final java.lang.reflect.Field field = Converters.class.getDeclaredField("converterMap");
        field.setAccessible(true);
        final java.util.Map<Class<?>, BiFunction<Object, Class<?>, Object>> map = (java.util.Map<Class<?>, BiFunction<Object, Class<?>, Object>>) field
                .get(null);
        final java.util.Map<Class<?>, BiFunction<Object, Class<?>, Object>> saved = new java.util.HashMap<>(map);

        try {
            map.clear();

            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(null));
            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(testfixtures.ConvertHierarchy.Child.class));
            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(testfixtures.ConvertHierarchy.Base.class));
            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(Integer.class));
            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(Object.class));
            assertEquals(5L, CommonUtil.convert(5, Long.class));

            final BiFunction<Object, Class<?>, Object> converter = (value, targetType) -> "from-base";
            map.put(testfixtures.ConvertHierarchy.Base.class, converter);

            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(null));
            org.junit.jupiter.api.Assertions.assertSame(converter, Converters.getConverter(testfixtures.ConvertHierarchy.Base.class));
            org.junit.jupiter.api.Assertions.assertSame(converter, Converters.getConverter(testfixtures.ConvertHierarchy.Child.class));
            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(Integer.class));
            org.junit.jupiter.api.Assertions.assertNull(Converters.getConverter(Object.class));
            assertEquals("from-base", CommonUtil.convert(new testfixtures.ConvertHierarchy.Child(), String.class));
        } finally {
            map.clear();
            map.putAll(saved);
        }
    }
    // ---- deep review 2026-11-02 G034 end ----
}
