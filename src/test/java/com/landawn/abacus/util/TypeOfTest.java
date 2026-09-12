package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

public class TypeOfTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    void exactClassesAndDynamicNamesRetainTheirRuntimeBehavior() {
        Type<Integer> integer = CommonUtil.typeOf(Integer.class);
        assertEquals(42, integer.valueOf("42"));
        assertEquals("42", integer.stringOf(42));
        Type<Integer> primitive = CommonUtil.typeOf(int.class);
        assertEquals(0, primitive.defaultValue());
        Type<? extends Number> producer = CommonUtil.typeOf(Integer.class);
        assertEquals(42, producer.valueOf("42"));
        Type<?> dynamic = CommonUtil.typeOf("java.lang.Integer");
        assertEquals(Integer.class, dynamic.javaType());
        assertEquals(42, dynamic.valueOf("42"));
        Type<List<String>> generic = new TypeReference<List<String>>() {
        }.type();
        assertEquals(List.of("\u4e2d", "\ud83d\ude00"), generic.valueOf("[\"\u4e2d\",\"\ud83d\ude00\"]"));
        assertEquals("\u4e2d\ud83d\ude00", CommonUtil.stringOf("\u4e2d\ud83d\ude00"));
        assertEquals("[1, 2]", CommonUtil.toString(new int[] { 1, 2 }));
        assertEquals("42", CommonUtil.convert(42, String.class));
        assertNull(CommonUtil.stringOf(null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((String) null));
    }

    @Test
    void compilerRejectsWrongClassesConsumerWideningAndNamedTypePromises() throws Exception {
        compile("SafeTypeOf", """
                Type<Integer> exact = N.typeOf(Integer.class);
                Type<? extends Number> producer = N.typeOf(Integer.class);
                Type<?> dynamic = N.typeOf("java.lang.String");
                Type<List<String>> generic = new TypeReference<List<String>>() {}.type();
                """, true);
        compile("WrongClassTypeOf", "Type<String> wrong = N.typeOf(Integer.class);", false);
        compile("WidenedConsumerTypeOf", "Type<Number> wrong = N.typeOf(Integer.class);", false);
        compile("NamedTypeOf", "Type<String> wrong = N.typeOf(\"java.lang.Integer\");", false);
    }

    private void compile(String name, String body, boolean expected) throws Exception {
        Path source = temp.resolve(name + ".java");
        Files.writeString(source, "import java.util.*;\nimport com.landawn.abacus.util.*;\nimport com.landawn.abacus.type.Type;\nclass " + name
                + " { void check() {\n" + body + "} }\n", StandardCharsets.UTF_8);
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler()
                .run(null, diagnostics, diagnostics, "-proc:none", "-classpath", classpath, "-d", temp.toString(), source.toString());
        String output = diagnostics.toString(StandardCharsets.UTF_8);
        assertEquals(expected, result == 0, output);
        if (!expected) {
            assertTrue(output.contains("incompatible"), output);
        }
    }
}
