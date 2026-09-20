package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import testfixtures.UtilConverterSources.Child;
import testfixtures.UtilConverterSources.FacadeSource;
import testfixtures.UtilConverterSources.Source;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class ConverterInputTypeTest extends TestBase {
    @TempDir
    Path directory;

    @Test
    void inferenceContravarianceHierarchyAndFirstRegistrationRemainSupported() {
        assertTrue(Converters.register(Source.class, (source, target) -> source.value));
        assertEquals("\uD83D\uDE00", CommonUtil.convert(new Source(), String.class));
        assertEquals("\uD83D\uDE00", CommonUtil.convert(new Child(), String.class));
        final BiFunction<Object, Class<?>, String> broad = (source, target) -> "replacement";
        assertFalse(Converters.register(Source.class, broad));
        assertEquals("\uD83D\uDE00", CommonUtil.convert(new Source(), String.class));
        assertTrue(CommonUtil.registerConverter(FacadeSource.class, (source, target) -> source.value));
        assertEquals("facade", CommonUtil.convert(new FacadeSource(), String.class));
        assertThrows(IllegalArgumentException.class, () -> Converters.register(Source.class, null));
        assertThrows(IllegalArgumentException.class, () -> Converters.register(null, broad));
    }

    @Test
    void incompatibleCallbacksFailAtCompileTimeThroughBothEntryPoints() throws Exception {
        UtilCycle2CompilationSupport.compile(directory, "ValidConverter", """
                class Source { String value; }
                Converters.register(Source.class, (value, target) -> value.value);
                java.util.function.BiFunction<Object, Class<?>, String> broad = (value, target) -> "ok";
                N.registerConverter(Source.class, broad);
                """, true);
        for (final String entry : new String[] { "Converters.register", "N.registerConverter" }) {
            UtilCycle2CompilationSupport.compile(directory, "InvalidConverter",
                    "class Source {}\njava.util.function.BiFunction<String, Class<?>, String> wrong = (s, t) -> s;\n" + entry + "(Source.class, wrong);",
                    false);
        }
    }
}
