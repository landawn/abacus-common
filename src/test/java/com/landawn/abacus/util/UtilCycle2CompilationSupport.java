package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.tools.ToolProvider;

final class UtilCycle2CompilationSupport {
    private UtilCycle2CompilationSupport() {
    }

    static void compile(final Path directory, final String name, final String body, final boolean expected) throws Exception {
        Files.createDirectories(directory);
        final Path source = directory.resolve(name + ".java");
        Files.writeString(source, "package com.landawn.abacus.util;\nimport java.util.*;\nclass " + name + " { void check() {\n" + body + "} }\n",
                StandardCharsets.UTF_8);
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final var diagnostics = new ByteArrayOutputStream();
        final int result = ToolProvider.getSystemJavaCompiler()
                .run(null, diagnostics, diagnostics, "-classpath", classpath, "-d", directory.toString(), source.toString());
        final String output = diagnostics.toString(StandardCharsets.UTF_8);
        assertEquals(expected, result == 0, output);
        if (!expected) {
            assertTrue(output.contains("incompatible"), output);
        }
    }
}
