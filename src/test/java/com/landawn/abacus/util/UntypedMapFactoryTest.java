package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class UntypedMapFactoryTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    void mixedPairsNullsAndDuplicatesRetainActualObjects() {
        Object[] tail = { 2, "\u4e2d\ud83d\ude00", null, null, "a", 2.5 };
        Map<Object, Object> hash = CommonUtil.toMap("a", 1, tail);
        Map<Object, Object> linked = CommonUtil.toLinkedHashMap("a", 1, tail);
        for (Map<Object, Object> map : List.of(hash, linked)) {
            assertEquals(3, map.size());
            assertEquals(2.5, map.get("a"));
            assertEquals("\u4e2d\ud83d\ude00", map.get(2));
            assertTrue(map.containsKey(null));
            assertNull(map.get(null));
            map.put("mutable", true);
            assertEquals(true, map.get("mutable"));
        }
        assertInstanceOf(LinkedHashMap.class, linked);
        assertEquals(Arrays.asList("a", 2, null, "mutable"), new ArrayList<>(linked.keySet()));
        tail[1] = "changed";
        assertEquals("\u4e2d\ud83d\ude00", hash.get(2));
        assertEquals("\u4e2d\ud83d\ude00", linked.get(2));
    }

    @Test
    void nullAndEmptyTailsAndFixedTypedOverloadsRemainSupported() {
        assertEquals(Map.of("a", 1), CommonUtil.toMap("a", 1, (Object[]) null));
        assertEquals(Map.of("a", 1), CommonUtil.toLinkedHashMap("a", 1, new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toMap("a", 1, new Object[] { "odd" }));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toLinkedHashMap("a", 1, new Object[] { "odd" }));
        Map<String, Integer> one = CommonUtil.toMap("a", 1);
        Map<String, Integer> two = CommonUtil.toMap("a", 1, "b", 2);
        Map<String, Integer> three = CommonUtil.toLinkedHashMap("a", 1, "b", 2, "c", 3);
        assertEquals(1, one.size());
        assertEquals(2, two.size());
        three.put("d", 4);
        assertEquals(List.of("a", "b", "c", "d"), new ArrayList<>(three.keySet()));
    }

    @Test
    void compilerRejectsTypedPromisesFromObjectVarargs() throws Exception {
        compile("SafeMapFactory", """
                Map<Object, Object> mixed = N.toMap("a", 1, new Object[] {"b", "text"});
                Map<Object, Object> many = N.toLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4);
                Map<String, Integer> typed = N.toLinkedHashMap("a", 1, "b", 2, "c", 3);
                typed.put("d", 4);
                """, true);
        compile("UnsafeHashFactory", "Map<String, Integer> wrong = N.toMap(\"a\", 1, new Object[] {\"b\", \"text\"});", false);
        compile("UnsafeLinkedFactory", "Map<String, Integer> wrong = N.toLinkedHashMap(\"a\", 1, \"b\", 2, \"c\", 3, \"d\", 4);", false);
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
