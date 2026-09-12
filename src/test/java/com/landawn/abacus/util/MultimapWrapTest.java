package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class MultimapWrapTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    public void interfaceValuedMapsRemainLiveAndAcceptNewCollections() {
        Map<String, List<String>> lists = new HashMap<>();
        ListMultimap<String, String> listMap = ListMultimap.wrap(lists);
        listMap.put("\u4e2d", null);
        listMap.put("\u4e2d", "\ud83d\ude00");
        assertEquals(Arrays.asList(null, "\ud83d\ude00"), lists.get("\u4e2d"));
        lists.get("\u4e2d").add("external");
        assertEquals(3, listMap.get("\u4e2d").size());

        Map<String, Set<String>> sets = new HashMap<>();
        SetMultimap<String, String> setMap = SetMultimap.wrap(sets);
        setMap.put(null, "\u4e2d");
        setMap.put(null, "\u4e2d");
        assertEquals(Set.of("\u4e2d"), sets.get(null));
        sets.get(null).add("external");
        assertEquals(2, setMap.get(null).size());
    }

    @Test
    public void explicitSuppliersPreserveSubtypeAcrossInsertionAndReplacementPaths() {
        Map<String, LinkedList<Integer>> lists = new HashMap<>();
        ListMultimap<String, Integer> listMap = ListMultimap.wrap(lists, LinkedList::new);
        listMap.put("put", 1);
        listMap.putValues("bulk", List.of(1, 2));
        listMap.computeIfAbsent("absent", key -> new ArrayList<>(List.of(3)));
        listMap.compute("compute", (key, old) -> new ArrayList<>(List.of(4)));
        listMap.merge("merge", List.of(5), (old, values) -> new ArrayList<>(values));
        listMap.compute("put", (key, old) -> new ArrayList<>(List.of(6)));
        lists.values().forEach(value -> assertEquals(LinkedList.class, value.getClass()));
        assertEquals(List.of(6), lists.get("put"));

        Map<String, TreeSet<Integer>> sets = new HashMap<>();
        SetMultimap<String, Integer> setMap = SetMultimap.wrap(sets, TreeSet::new);
        setMap.put("put", 1);
        setMap.putValues("bulk", List.of(2, 1, 2));
        setMap.computeIfAbsent("absent", key -> new HashSet<>(List.of(3)));
        setMap.compute("compute", (key, old) -> new HashSet<>(List.of(4)));
        setMap.merge("merge", List.of(5), (old, values) -> new HashSet<>(values));
        setMap.compute("put", (key, old) -> new HashSet<>(List.of(6)));
        sets.values().forEach(value -> assertEquals(TreeSet.class, value.getClass()));
        assertEquals(List.of(1, 2), new ArrayList<>(sets.get("bulk")));
    }

    @Test
    public void validationAndExistingImmutableValuesRetainTheirBehavior() {
        Map<String, List<Integer>> lists = new HashMap<>();
        lists.put("fixed", List.of(1));
        ListMultimap<String, Integer> listMap = ListMultimap.wrap(lists);
        listMap.put("new", 2);
        assertEquals(List.of(2), lists.get("new"));
        assertThrows(UnsupportedOperationException.class, () -> listMap.put("fixed", 2));
        lists.put("empty", List.of());
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(lists));
        Map<String, Set<Integer>> sets = new HashMap<>();
        sets.put("null", null);
        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(sets));
        assertThrows(IllegalArgumentException.class, () -> ListMultimap.wrap(null));
        assertThrows(IllegalArgumentException.class, () -> SetMultimap.wrap(null));
    }

    @Test
    public void compilerRejectsCovariantMapsAndAcceptsExplicitSupplierMigration() throws Exception {
        assertCompilation("SafeWrap", """
                Map<String, LinkedList<Integer>> lists = new HashMap<>();
                Map<String, TreeSet<Integer>> sets = new HashMap<>();
                ListMultimap.wrap(lists, LinkedList::new);
                SetMultimap.wrap(sets, TreeSet::new);
                """, true);
        assertCompilation("UnsafeListWrap", """
                Map<String, LinkedList<Integer>> lists = new HashMap<>();
                ListMultimap.wrap(lists);
                """, false);
        assertCompilation("UnsafeSetWrap", """
                Map<String, TreeSet<Integer>> sets = new HashMap<>();
                SetMultimap.wrap(sets);
                """, false);
    }

    private void assertCompilation(String name, String body, boolean expectedSuccess) throws Exception {
        Path source = temp.resolve(name + ".java");
        Files.writeString(source, "import java.util.*;\nimport com.landawn.abacus.util.*;\nclass " + name + " { void check() {\n" + body + "} }\n",
                StandardCharsets.UTF_8);
        var compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager files = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
            boolean success = compiler
                    .getTask(null, files, diagnostics, List.of("-classpath", classpath, "-d", temp.toString()), null, files.getJavaFileObjects(source))
                    .call();
            assertEquals(expectedSuccess, success, diagnostics.getDiagnostics().toString());
            if (!expectedSuccess) {
                assertTrue(
                        diagnostics.getDiagnostics()
                                .stream()
                                .anyMatch(d -> d.getKind() == Diagnostic.Kind.ERROR && d.getMessage(java.util.Locale.ROOT).contains("wrap")),
                        diagnostics.getDiagnostics().toString());
            }
        }
    }
}
