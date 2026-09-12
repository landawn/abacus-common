package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Tag("unit")
public class TypeAttrClassTokenTest {
    @TempDir
    Path directory;

    @Test
    void matchingAndDynamicTokensPreserveConstructorSelection() {
        final StringBuilder exact = TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder", new Object[0]);
        assertEquals("", exact.toString());
        final StringBuilder parsed = TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder(\u03B1)", new Object[0]);
        assertEquals("\u03B1", parsed.toString());
        final StringBuilder explicit = TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder", String.class, "\uD83D\uDE00");
        assertEquals("\uD83D\uDE00", explicit.toString());
        final Object dynamic = TypeAttrParser.newInstance(null, "java.lang.StringBuilder", new Object[0]);
        assertInstanceOf(StringBuilder.class, dynamic);
        assertEquals("", TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder").toString());
        assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.newInstance(StringBuilder.class, null, new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder", (Object[]) null));
        assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder", String.class));
    }

    @Test
    void incompatibleClassTokensAreRejectedByTheCompiler() throws Exception {
        UtilCycle2CompilationSupport.compile(directory, "ValidTypeAttr",
                "StringBuilder value = TypeAttrParser.newInstance(StringBuilder.class, \"StringBuilder\", new Object[0]); Object dynamic = TypeAttrParser.newInstance(null, \"java.lang.StringBuilder\", new Object[0]);",
                true);
        UtilCycle2CompilationSupport.compile(directory, "InvalidTypeAttr",
                "String value = TypeAttrParser.newInstance(StringBuilder.class, \"StringBuilder\", new Object[0]);", false);
    }
}
