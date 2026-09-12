package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class FlattenLeafTypeTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    void mixedLeavesPreserveOrderIdentityNullsAndCollectionSemantics() {
        Object marker = new Object();
        int[] arrayLeaf = { 1, 2 };
        Iterable<?> lazy = () -> Arrays.asList("\u4e2d\ud83d\ude00", marker).iterator();
        List<?> input = Arrays.asList(Arrays.asList("a", 1, null), List.of(), lazy, arrayLeaf, "a");
        List<Object> expected = Arrays.asList("a", 1, null, "\u4e2d\ud83d\ude00", marker, arrayLeaf, "a");
        ArrayList<Object> result = N.flattenEachElement(input, ArrayList<Object>::new);
        assertEquals(expected, result);
        assertSame(marker, result.get(4));
        assertSame(arrayLeaf, result.get(5));
        assertEquals(expected, N.flattenEachElement(input));
        LinkedHashSet<Object> set = N.flattenEachElement(input, LinkedHashSet<Object>::new);
        assertEquals(new ArrayList<>(new LinkedHashSet<>(expected)), new ArrayList<>(set));
    }

    @Test
    void supplierIsInvokedOnceAndItsExistingContentsAreRetained() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Object> supplied = new ArrayList<>(List.of("prefix"));
        assertSame(supplied, N.flattenEachElement(null, () -> {
            calls.incrementAndGet();
            return supplied;
        }));
        assertEquals(1, calls.get());
        assertSame(supplied, N.flattenEachElement(List.of(), () -> supplied));
        assertSame(supplied, N.flattenEachElement(List.of(List.of(1)), () -> supplied));
        assertEquals(List.of("prefix", 1), supplied);
        assertTrue(N.flattenEachElement(null).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.flattenEachElement(null, null));
    }

    @Test
    void compilerRejectsUnfoundedLeafTypesAndAcceptsExplicitCheckedMigration() throws Exception {
        compile("SafeFlatten", """
                ArrayList<Object> values = N.flattenEachElement(List.of(List.of("a", 1)), ArrayList<Object>::new);
                List<String> strings = N.flattenEachElement(List.of(List.of("a")), ArrayList<Object>::new)
                    .stream().map(String.class::cast).toList();
                """, true);
        compile("UnsafeFlatten", "List<String> values = N.flattenEachElement(List.of(List.of(1)), ArrayList<String>::new);", false);
    }

    private void compile(String name, String body, boolean expected) throws Exception {
        Path source = temp.resolve(name + ".java");
        Files.writeString(source, "import java.util.*;\nimport com.landawn.abacus.util.N;\nclass " + name + " { void check() {\n" + body + "} }\n",
                StandardCharsets.UTF_8);
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler()
                .run(null, diagnostics, diagnostics, "-classpath", classpath, "-d", temp.toString(), source.toString());
        String output = diagnostics.toString(StandardCharsets.UTF_8);
        assertEquals(expected, result == 0, output);
        if (!expected) {
            assertTrue(output.contains("incompatible"), output);
        }
    }
}
