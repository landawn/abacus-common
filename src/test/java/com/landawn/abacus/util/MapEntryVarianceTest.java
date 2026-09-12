package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class MapEntryVarianceTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    void entrySetRetainsWriteThroughAndLiveMembership() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("\u4e2d", 1);
        Set<Map.Entry<String, Integer>> entries = Maps.entrySet(source);
        assertEquals(1, entries.iterator().next().setValue(2));
        assertEquals(2, source.get("\u4e2d"));
        source.put(null, null);
        assertEquals(2, entries.size());
        assertTrue(entries.contains(new java.util.AbstractMap.SimpleEntry<>(null, null)));
        assertThrows(UnsupportedOperationException.class, entries::clear);
        assertThrows(UnsupportedOperationException.class, entries.iterator()::remove);
        Map<String, Integer> initiallyEmpty = new HashMap<>();
        Set<Map.Entry<String, Integer>> emptyResult = Maps.entrySet(initiallyEmpty);
        initiallyEmpty.put("later", 1);
        assertTrue(emptyResult.isEmpty());
        assertTrue(Maps.entrySet(null).isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void concatenationKeepsEntriesMutableAndObtainsLaterIteratorsLazily() {
        Map<String, Integer> first = new LinkedHashMap<>();
        Map<String, Integer> later = new LinkedHashMap<>();
        first.put("a", 1);
        later.put("b", 2);
        Map<String, Integer>[] maps = new Map[] { first, null, Map.of(), later };
        ObjIterator<Map.Entry<String, Integer>> iter = Iterators.concat(maps);
        maps[3] = Map.of("replacement", 99);
        assertEquals("a", iter.next().getKey());
        later.put("\ud83d\ude00", 3);
        Map.Entry<String, Integer> entry = iter.next();
        assertEquals("b", entry.getKey());
        entry.setValue(4);
        assertEquals(4, later.get("b"));
        assertEquals("\ud83d\ude00", iter.next().getKey());
        assertFalse(iter.hasNext());
        assertFalse(Iterators.concat((Map<String, Integer>[]) null).hasNext());
        assertThrows(UnsupportedOperationException.class, () -> Iterators.concat(Map.of("x", 1)).next().setValue(2));
    }

    @Test
    void compilerRejectsWidenedWritableEntriesAndAcceptsCopiedEntries() throws Exception {
        compile("SafeEntryCopy", """
                Map<String, Integer> map = new HashMap<>();
                Set<Map.Entry<String, Integer>> entries = Maps.entrySet(map);
                ObjIterator<Map.Entry<String, Integer>> iterator = Iterators.concat(map);
                List<Map.Entry<String, Number>> copies = map.entrySet().stream()
                    .<Map.Entry<String, Number>>map(e -> new AbstractMap.SimpleImmutableEntry<String, Number>(e.getKey(), e.getValue())).toList();
                """, true);
        compile("UnsafeEntrySet", """
                Map<String, Integer> map = new HashMap<>();
                Set<Map.Entry<String, Number>> entries = Maps.entrySet(map);
                """, false);
        compile("UnsafeEntryConcat", """
                Map<String, Integer> map = new HashMap<>();
                ObjIterator<Map.Entry<String, Number>> entries = Iterators.concat(map);
                """, false);
    }

    private void compile(String name, String body, boolean expected) throws Exception {
        Path source = temp.resolve(name + ".java");
        Files.writeString(source, "import java.util.*;\nimport com.landawn.abacus.util.*;\nclass " + name + " { void check() {\n" + body + "} }\n",
                StandardCharsets.UTF_8);
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
        int result = ToolProvider.getSystemJavaCompiler()
                .run(null, diagnostics, diagnostics, "-classpath", classpath, "-d", temp.toString(), source.toString());
        String output = diagnostics.toString(StandardCharsets.UTF_8);
        assertEquals(expected, result == 0, output);
        if (!expected) {
            assertTrue(output.contains("incompatible") || output.contains("no suitable method"), output);
        }
    }
}
