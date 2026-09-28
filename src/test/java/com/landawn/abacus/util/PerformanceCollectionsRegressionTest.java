package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
public class PerformanceCollectionsRegressionTest {
    private static volatile Object allocationSink;

    @Test
    public void duplicateCompositeKeysReuseUnretainedCandidates() throws Exception {
        allocated(() -> { }); // Skip consistently if this VM cannot report thread allocation.
        // Concrete Mockito mocks instrument Object.equals for the rest of the suite, adding
        // dispatch allocations to array-key comparisons. Measure production code in a fresh VM.
        final String executable = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final java.nio.file.Path output = java.nio.file.Files.createTempFile("dedup-allocation-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(executable, "-cp", classpath, DuplicateKeyAllocationProcess.class.getName())
                    .redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "allocation probe did not terminate");
            assertEquals(0, process.exitValue(), java.nio.file.Files.readString(output));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            java.nio.file.Files.deleteIfExists(output);
        }
    }

    public static final class DuplicateKeyAllocationProcess {
        public static void main(String[] args) {
            verifyDuplicateKeyCandidateAllocation();
        }
    }

    private static void verifyDuplicateKeyCandidateAllocation() {
        for (int i = 0; i < 5; i++) {
            dataset(List.of("a", "b"), Collections.nCopies(1000, 1), Collections.nCopies(1000, "x"))
                    .removeDuplicateRowsBy(List.of("a", "b"));
        }
        final RowDataset data = dataset(List.of("a", "b"), Collections.nCopies(100000, 1), Collections.nCopies(100000, "x"));
        final long bytes = allocated(() -> data.removeDuplicateRowsBy(List.of("a", "b")));
        assertEquals(1, data.size());
        assertEquals(List.of(1), data.getColumn("a"));
        assertEquals(List.of("x"), data.getColumn("b"));
        // A repeated compound key must not allocate one candidate array/wrapper per input row.
        assertTrue(bytes < 150000, "duplicate compound keys allocated " + bytes + " bytes");
    }

    @Test
    public void simpleKeyDedupPreservesNullNanSignedZeroAndNoOpViews() {
        for (boolean compound : new boolean[] { false, true }) {
            final RowDataset data = dataset(List.of("k", "tag", "v"),
                    Arrays.asList(null, null, Double.NaN, Double.longBitsToDouble(0x7ff8000000000001L), -0.0, +0.0, -0.0),
                    Collections.nCopies(7, "same"), List.of("null", "duplicate", "nan", "duplicate", "negative", "positive", "duplicate"));
            if (compound) {
                data.removeDuplicateRowsBy(List.of("k", "tag"));
            } else {
                data.removeDuplicateRowsBy("k");
            }
            assertEquals(List.of("null", "nan", "negative", "positive"), data.getColumn("v"));
            final Dataset slice = data.slice(0, data.size());
            data.removeDuplicateRowsBy(List.of("k", "tag"));
            assertEquals(data.size(), slice.size(), "a no-op must not invalidate an existing slice");
        }
    }

    @Test
    public void customHashKeysKeepPerRowCellSnapshots() {
        final AtomicReference<RowDataset> owner = new AtomicReference<>();
        class Key {
            final int value;
            Key(int value) { this.value = value; }
            @Override public int hashCode() {
                if (value == 2) { owner.get().set(0, 1, "after"); }
                return value;
            }
            @Override public boolean equals(Object other) { return other instanceof Key key && key.value == value; }
        }
        final RowDataset data = dataset(List.of("k", "v"), List.of(new Key(1), new Key(2), new Key(1)), List.of("before", "second", "duplicate"));
        owner.set(data);
        data.removeDuplicateRowsBy("k");
        assertEquals(List.of("before", "second"), data.getColumn("v"));
    }

    @Test
    public void sparseMapSizingPreservesCustomConstructorBounds() {
        final MapsReview20260924Test.Lru<Integer, Integer> source = new MapsReview20260924Test.Lru<>(100);
        final MapsReview20260924Test.Lru<Integer, List<Integer>> columns = new MapsReview20260924Test.Lru<>(100);
        for (int i = 0; i < 100; i++) {
            source.put(i, i);
            columns.put(i, List.of(i));
        }
        assertEquals(source, Maps.filter(source, entry -> true));
        assertEquals(source, Maps.filter(source, (key, value) -> true));
        assertEquals(source, Maps.filterByKey(source, key -> true));
        assertEquals(source, Maps.filterByValue(source, value -> true));
        assertEquals(List.of(source), Maps.transpose(columns));
    }

    @Test
    public void allFlatMapAritiesAlignCopiedColumnsForNullEmptyAndExpandedRows() {
        final RowDataset source = dataset(List.of("a", "b", "c", "copy"), List.of(1, 2, 3), List.of(10, 20, 30),
                List.of(100, 200, 300), List.of("one", "two", "three"));
        final java.util.function.Function<Object, List<?>> values = value -> Integer.valueOf(1).equals(value) ? null
                : Integer.valueOf(2).equals(value) ? List.of() : Arrays.asList(value, null);
        final List<Dataset> results = List.of(
                source.flatMapColumn("a", "out", List.of("copy"), values),
                source.flatMapColumns(Tuple.of("a", "b"), "out", List.of("copy"), (a, b) -> values.apply(a)),
                source.flatMapColumns(Tuple.of("a", "b", "c"), "out", List.of("copy"), (a, b, c) -> values.apply(a)),
                source.flatMapColumns(List.of("a", "b", "c"), "out", List.of("copy"), row -> values.apply(row.get(0))));
        for (Dataset result : results) {
            assertEquals(List.of("copy", "out"), result.columnNames());
            assertEquals(List.of("three", "three"), result.getColumn("copy"));
            assertEquals(Arrays.asList(3, null), result.getColumn("out"));
            result.set(0, 0, "changed");
            assertEquals("three", source.get(2, 3));
        }
    }

    @Test
    public void ownedColumnFactoryValidatesShapeAndProducesMutableIndependentColumns() {
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(null, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a"), null));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a", "a"), List.of(List.of(1), List.of(2))));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a", "b"), List.of(List.of(1), List.of())));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a"), new ArrayList<>()));
        final RowDataset data = RowDataset.fromOwnedColumns(new ArrayList<>(List.of("a", "b")),
                new ArrayList<>(List.of(new ArrayList<>(List.of(1)), new ArrayList<>(List.of(2)))));
        data.addRow(new Object[] { 3, 4 });
        data.set(0, 0, 5);
        data.renameColumn("a", "renamed");
        assertEquals(List.of("renamed", "b"), data.columnNames());
        assertEquals(List.of(5, 3), data.getColumn(0));
        assertEquals(List.of(2, 4), data.getColumn(1));
    }

    @Test
    public void ownedColumnFactoryWithPropertiesAdoptsColumnsAndCopiesProperties() {
        final Map<String, Object> props = Map.of("source", "x");
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(null, new ArrayList<>(), props));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a"), null, props));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a", "a"), List.of(List.of(1), List.of(2)), props));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of("a", "b"), List.of(List.of(1), List.of()), props));
        assertThrows(IllegalArgumentException.class, () -> RowDataset.fromOwnedColumns(List.of(""), List.of(List.of(1)), props));

        final List<Object> firstColumn = new ArrayList<>(List.of(1));
        final List<List<Object>> columns = new ArrayList<>(List.of(firstColumn, new ArrayList<>(List.of(2))));
        final java.util.TreeMap<String, Object> properties = new java.util.TreeMap<>(Map.of("z", 26, "a", 1));
        final RowDataset data = RowDataset.fromOwnedColumns(new ArrayList<>(List.of("a", "b")), columns, properties);

        // The table storage is adopted, not copied: the dataset writes through to the supplied column list.
        data.set(0, 0, 5);
        assertEquals(List.of(5), firstColumn);
        data.addRow(new Object[] { 3, 4 });
        assertEquals(List.of(5, 3), data.getColumn(0));
        assertEquals(List.of(2, 4), data.getColumn(1));

        // The properties are copied, keeping the map's ordering; later changes to the supplied map do not show.
        assertEquals(List.of("a", "z"), new ArrayList<>(data.getProperties().keySet()));
        properties.put("b", 2);
        properties.remove("z");
        assertEquals(Map.of("a", 1, "z", 26), data.getProperties());
        assertThrows(UnsupportedOperationException.class, () -> data.getProperties().put("c", 3));

        // null or empty properties mean no properties, like the two-argument overload.
        assertTrue(RowDataset.fromOwnedColumns(new ArrayList<>(List.of("a")), new ArrayList<>(List.of(new ArrayList<>())), null).getProperties().isEmpty());
        assertTrue(RowDataset.fromOwnedColumns(new ArrayList<>(List.of("a")), new ArrayList<>(List.of(new ArrayList<>())), new HashMap<>()).getProperties().isEmpty());
        assertTrue(RowDataset.fromOwnedColumns(new ArrayList<>(List.of("a")), new ArrayList<>(List.of(new ArrayList<>()))).getProperties().isEmpty());
    }

    @Test
    public void excelImportOwnsColumnsWithoutRetainingCallbackBuffers() throws Exception {
        final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        try (org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook()) {
            final org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("data");
            sheet.createRow(0).createCell(0).setCellValue("name");
            sheet.createRow(1).createCell(0).setCellValue("first");
            sheet.createRow(2).createCell(0).setCellValue("second");
            workbook.write(bytes);
        }
        final AtomicReference<String[]> header = new AtomicReference<>();
        final AtomicReference<Object[]> output = new AtomicReference<>();
        final Dataset data = com.landawn.abacus.poi.ExcelUtil.readDatasetFromSheet(new java.io.ByteArrayInputStream(bytes.toByteArray()), "data",
                (names, row, values) -> {
                    header.set(names);
                    output.set(values);
                    values[0] = row.getCell(0).getStringCellValue();
                });
        header.get()[0] = "external";
        output.get()[0] = "external";
        assertEquals(List.of("name"), data.columnNames());
        assertEquals(List.of("first", "second"), data.getColumn(0));
        data.addRow(new Object[] { "third" });
        assertEquals(List.of("first", "second", "third"), data.getColumn(0));
    }

    public static final class ListingProcess {
        public static void main(String[] args) {
            if (args.length > 0) {
                if (args[0].equals("blank")) {
                    System.out.println("   ");
                    System.out.println("\t");
                }
                return;
            }
            for (int i = 0; i < 100_000; i++) {
                System.out.println("Entry " + i);
            }
            System.out.println("  FINAL SPACE SUMMARY  ");
            System.out.println("   ");
        }
    }

    @Test
    public void commandTailCollectionDrainsOutputAndRetainsOnlyLastNonBlankLine() throws Exception {
        final var constructor = FileSystemUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final FileSystemUtil fileSystem = constructor.newInstance();
        final String executable = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final String testClasses = java.nio.file.Path.of(ListingProcess.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        final String[] command = { executable, "-cp", testClasses, ListingProcess.class.getName() };
        final var method = FileSystemUtil.class.getDeclaredMethod("performCommand", String[].class, int.class, long.class, Map.class, boolean.class);
        method.setAccessible(true);
        assertEquals(List.of("final space summary"), method.invoke(fileSystem, command, 1, 15_000L, null, true));
        assertEquals(List.of("entry 0"), fileSystem.performCommand(command, 1, 15_000));
    }

    @Test
    public void commandTailPreservesTheDistinctionBetweenBlankAndAbsentOutput() throws Exception {
        final var constructor = FileSystemUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        final FileSystemUtil fileSystem = constructor.newInstance();
        final String executable = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final String testClasses = java.nio.file.Path.of(ListingProcess.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        final var method = FileSystemUtil.class.getDeclaredMethod("performCommand", String[].class, int.class, long.class, Map.class, boolean.class);
        method.setAccessible(true);
        final String[] blank = { executable, "-cp", testClasses, ListingProcess.class.getName(), "blank" };
        assertEquals(List.of(""), method.invoke(fileSystem, blank, 1, 15000L, null, true));
        final String[] empty = { executable, "-cp", testClasses, ListingProcess.class.getName(), "empty" };
        final var failure = assertThrows(java.lang.reflect.InvocationTargetException.class,
                () -> method.invoke(fileSystem, empty, 1, 15000L, null, true));
        assertInstanceOf(java.io.IOException.class, failure.getCause());
        assertTrue(failure.getCause().getMessage().contains("did not return any info"));
    }

    @Test
    public void blockedSourceCallbacksDoNotStarveUnrelatedVirtualThreads() throws Exception {
        // One carrier makes JDK 21 monitor pinning deterministic without starting one callback per CPU.
        final String executable = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "java").toString();
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final java.nio.file.Path output = java.nio.file.Files.createTempFile("observer-blocking-callback-", ".log");
        Process process = null;
        try {
            process = new ProcessBuilder(executable, "-Djdk.virtualThreadScheduler.parallelism=1", "-Djdk.virtualThreadScheduler.maxPoolSize=1",
                    "-cp", classpath, BlockingCallbackProcess.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "blocking callback probe did not terminate");
            assertEquals(0, process.exitValue(), java.nio.file.Files.readString(output));
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
            java.nio.file.Files.deleteIfExists(output);
        }
    }

    public static final class BlockingCallbackProcess {
        public static void main(String[] args) throws Exception {
            for (boolean queueSource : new boolean[] { true, false }) {
                final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>(List.of(1));
                final Observer<Integer> observer = (queueSource ? Observer.of(queue) : Observer.of(List.of(1))).limit(1);
                final CountDownLatch entered = new CountDownLatch(1);
                final CountDownLatch release = new CountDownLatch(1);
                final CountDownLatch completed = new CountDownLatch(1);
                final CountDownLatch unrelatedProgress = new CountDownLatch(1);
                final AtomicReference<Exception> error = new AtomicReference<>();
                observer.observe(value -> {
                    entered.countDown();
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(e);
                    }
                }, e -> { error.set(e); completed.countDown(); }, completed::countDown);
                try {
                    assertTrue(entered.await(10, TimeUnit.SECONDS), "source callback did not start");
                    Thread.ofVirtual().start(unrelatedProgress::countDown);
                    assertTrue(unrelatedProgress.await(5, TimeUnit.SECONDS), "blocked source callback starved an unrelated virtual thread");
                } finally {
                    release.countDown();
                    assertTrue(completed.await(5, TimeUnit.SECONDS), "source did not complete after releasing its callback");
                }
                assertNull(error.get());
            }
        }
    }

    @Test
    public void waitingQueueSourcesDoNotStarveOtherSubscriptions() throws Exception {
        final int count = Math.max(64, IOUtil.CPU_CORES * 8) + 1;
        final List<BlockingQueue<Integer>> queues = new ArrayList<>();
        final CountDownLatch started = new CountDownLatch(count);
        final CountDownLatch completed = new CountDownLatch(count);
        final AtomicReference<Exception> error = new AtomicReference<>();
        try {
            for (int i = 0; i < count; i++) {
                final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();
                queues.add(queue);
                queue.add(i);
                Observer.of(queue).observe(value -> started.countDown(), error::set, completed::countDown);
            }
            assertTrue(started.await(15, TimeUnit.SECONDS), "queue subscriptions must start even while earlier sources remain open");
            final CountDownLatch finite = new CountDownLatch(1);
            Observer.of(List.of(42)).observe(value -> assertEquals(42, value), error::set, finite::countDown);
            assertTrue(finite.await(5, TimeUnit.SECONDS));
            assertNull(error.get());
        } finally {
            queues.forEach(Observer::complete);
            assertTrue(completed.await(15, TimeUnit.SECONDS));
        }
    }

    @Test
    public void distinctReleasesHistoryOnCompletionLimitAndError() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            final Observer<Integer> observer = mode == 1 ? Observer.of(List.of(1, 2, 3)).distinct().limit(1)
                    : mode == 2 ? Observer.of(List.of(1, 2, 3)).distinctBy(value -> {
                        if (value == 2) {
                            throw new IllegalArgumentException("expected");
                        }
                        return value;
                    }) : Observer.of(List.of(1, 2, 1)).distinct();
            final CountDownLatch terminal = new CountDownLatch(1);
            final List<Integer> values = new ArrayList<>();
            final AtomicReference<Exception> error = new AtomicReference<>();
            observer.observe(values::add, e -> { error.set(e); terminal.countDown(); }, terminal::countDown);
            assertTrue(terminal.await(5, TimeUnit.SECONDS));
            synchronized (field(observer, "eventGate")) {
                Object dispatcher = observer.dispatcher;
                boolean found = false;
                while (dispatcher != null) {
                    try {
                        final Field set = dispatcher.getClass().getDeclaredField("set");
                        set.setAccessible(true);
                        assertNull(set.get(dispatcher), "terminal cleanup must release entries and backing storage");
                        found = true;
                    } catch (NoSuchFieldException ignored) {
                        // Not a distinct dispatcher.
                    }
                    dispatcher = field(dispatcher, "downDispatcher");
                }
                assertTrue(found);
            }
            assertEquals(mode == 0 ? List.of(1, 2) : List.of(1), values);
            assertEquals(mode == 2, error.get() != null);
        }
    }

    @Test
    public void distinctReleasesHistoryWhenDeliveryOrTerminalCallbacksFailFatally() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            final int failureMode = mode;
            final AssertionError fatal = new AssertionError("expected fatal callback");
            final AtomicReference<Throwable> uncaught = new AtomicReference<>();
            final CountDownLatch stopped = new CountDownLatch(1);
            final java.util.concurrent.atomic.AtomicInteger terminalCalls = new java.util.concurrent.atomic.AtomicInteger();
            final Observer<Integer> observer = mode == 1 ? Observer.of(List.of(1)).distinct() : Observer.of(List.of(1)).distinctBy(value -> value);
            observer.observe(value -> {
                Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> {
                    uncaught.set(failure);
                    stopped.countDown();
                });
                if (failureMode == 0) { throw fatal; }
                if (failureMode == 2) { throw new IllegalStateException("expected source callback failure"); }
            }, error -> {
                terminalCalls.incrementAndGet();
                throw fatal;
            }, () -> {
                terminalCalls.incrementAndGet();
                throw fatal;
            });
            assertTrue(stopped.await(5, TimeUnit.SECONDS));
            assertSame(fatal, uncaught.get());
            assertEquals(mode == 0 ? 0 : 1, terminalCalls.get());
            synchronized (field(observer, "eventGate")) {
                final Object distinct = field(observer.dispatcher, "downDispatcher");
                assertNull(field(distinct, "set"), "fatal cleanup must release the distinct table");
            }
        }
    }

    @Test
    public void longestPrefixValidatesTheWholeKeyAndSupportsSequentialLists() {
        final PrefixSearchTable.Builder<String, Integer> builder = PrefixSearchTable.builder();
        final LinkedList<String> key = new LinkedList<>() {
            @Override public String get(int index) { throw new AssertionError("indexed traversal of a sequential key"); }
        };
        key.addAll(List.of("a", "b", "c"));
        builder.add(List.of("a"), 1).add(key, 3);
        final PrefixSearchTable<String, Integer> table = builder.build();
        assertEquals(3, table.get(List.of("a", "b", "c", "d")).get());
        assertEquals(1, table.get(List.of("a", "x")).get());
        assertTrue(table.get(List.of("x")).isEmpty());
        assertThrows(NullPointerException.class, () -> table.get(Arrays.asList("missing", null)));
        assertThrows(NullPointerException.class, () -> builder.add(Arrays.asList("z", null), 4));
        assertTrue(builder.build().get(List.of("z")).isEmpty());
    }

    @Test
    public void regexLastMatchPreservesZeroWidthAndUnicodeBounds() {
        for (String source : List.of("", "abc", "a\uD83D\uDE00b\uD83D\uDE00", "x x x")) {
            for (String expression : List.of("x", "$", "(?=.)", ".", "nomatch")) {
                final java.util.regex.Matcher matcher = Pattern.compile(expression).matcher(source);
                String expected = null;
                while (matcher.find()) {
                    expected = matcher.group();
                }
                assertEquals(expected, RegExUtil.findLast(source, Pattern.compile(expression)));
            }
        }
        assertNull(RegExUtil.findLast(null, Pattern.compile(".")));
    }

    @Test
    public void ordinaryKahanCombineDoesNotAllocateExactDecimalAggregates() {
        for (int i = 0; i < 4; i++) {
            combineFinite(1000);
        }
        final long bytes = allocated(() -> combineFinite(10000));
        assertTrue(bytes < 1_000_000, "finite combines allocated " + bytes + " bytes");
        final KahanSummation sum = KahanSummation.of(Double.MAX_VALUE, 1, Double.MAX_VALUE);
        sum.combine(KahanSummation.of(-Double.MAX_VALUE, -Double.MAX_VALUE));
        assertEquals(0.2, sum.average().get(), 0.0);
    }

    @Test
    public void normalizationCompactsLongReduciblePathsAndPreservesPrefixes() {
        assertEquals("x/", FilenameUtil.normalize("x/" + "./".repeat(20000), true));
        assertEquals("x/", FilenameUtil.normalize("x/" + "a/../".repeat(20000), true));
        assertEquals("x", FilenameUtil.normalizeNoEndSeparator("x/" + "a/../".repeat(20000), true));
        assertEquals("//host/share/", FilenameUtil.normalize("//host//share/a/../", true));
        assertEquals("C:/x", FilenameUtil.normalize("C:\\\\a\\..\\x", true));
        assertEquals("~user/", FilenameUtil.normalize("~user", true));
        assertNull(FilenameUtil.normalize("C://../x", true));
        assertNull(FilenameUtil.normalize("a/../../x", true));
        assertEquals("", FilenameUtil.normalize("a/..", true));
        assertThrows(IllegalArgumentException.class, () -> FilenameUtil.normalize("x\0y"));
    }

    @Test
    public void groupingAndSparseMapTransformsPreserveValuesAndOrdering() {
        final List<Integer> source = Arrays.asList(1, 2, 1, null);
        assertEquals(Arrays.asList(1, 2, 1, null), ListMultimap.fromCollection(source, value -> "key").get("key"));
        assertEquals(new java.util.HashSet<>(source), SetMultimap.fromCollection(source, value -> "key").get("key"));
        assertEquals(2, Multiset.of(1, 2, 1, null).count(1));
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("first", 1); map.put("second", 2); map.put("third", 3);
        assertEquals(List.of("first", "third"), new ArrayList<>(Maps.filterByValue(map, value -> value != 2).keySet()));
        final Map<String, List<Integer>> columns = new LinkedHashMap<>();
        columns.put("a", List.of(1, 2, 3)); columns.put("b", List.of(4));
        assertEquals(List.of(Map.of("a", 1, "b", 4), Map.of("a", 2), Map.of("a", 3)), Maps.transpose(columns));
        final Map<Integer, Integer> large = new HashMap<>();
        for (int i = 0; i < 100000; i++) { large.put(i, i); }
        Maps.filterByKey(large, key -> key == 0);
        final long bytes = allocated(() -> allocationSink = Maps.filterByKey(large, key -> key == 0));
        assertTrue(bytes < 100000, "a one-entry result should not allocate an input-sized table: " + bytes);
    }

    @Test
    public void sheetColumnInsertionKeepsCallerAndSelfViewsIsolated() {
        final Sheet<Integer, String, Integer> sheet = new Sheet<>(List.of(1, 2), List.of("a"));
        sheet.set(1, "a", 10); sheet.set(2, "a", 20);
        final List<Integer> values = new ArrayList<>(List.of(30, 40));
        sheet.addColumn("b", values);
        values.set(0, 999);
        assertEquals(List.of(30, 40), sheet.columnValues("b"));
        sheet.addColumn(0, "copy", sheet.columnValues("a"));
        sheet.set(1, "a", 99);
        assertEquals(List.of(10, 20), sheet.columnValues("copy"));
        sheet.addColumn(sheet.columnCount(), "end", sheet.columnValues("b"));
        assertEquals(List.of(30, 40), sheet.columnValues("end"));
        final int columns = sheet.columnCount();
        assertThrows(IllegalArgumentException.class, () -> sheet.addColumn(0, "invalid", List.of(1)));
        assertEquals(columns, sheet.columnCount());
    }

    @Test
    public void datasetDedupKeepsOrderLiveViewsAndCustomExtractorSnapshots() {
        final RowDataset data = dataset(List.of("k", "v"), List.of(1, 2, 1, 3), List.of("a", "b", "c", "d"));
        final List<Object> view = data.getColumn("v");
        data.removeDuplicateRowsBy("k");
        assertEquals(List.of(1, 2, 3), data.getColumn("k"));
        assertEquals(List.of("a", "b", "d"), view);
        final RowDataset multiple = dataset(List.of("a", "b", "v"), List.of(1, 1, 1), List.of(2, 2, 3), List.of("x", "y", "z"));
        multiple.removeDuplicateRowsBy(List.of("a", "b"));
        assertEquals(List.of("x", "z"), multiple.getColumn("v"));
        final RowDataset sideEffect = dataset(List.of("k", "v"), List.of(1, 2, 1), List.of("before", "b", "c"));
        sideEffect.removeDuplicateRowsBy("k", value -> {
            if (Integer.valueOf(2).equals(value)) { sideEffect.set(0, 1, "after"); }
            return value;
        });
        assertEquals(List.of("before", "b"), sideEffect.getColumn("v"));
    }

    @Test
    public void emptyDatasetSetOperationsValidateSchemaWithoutIndexingRightRows() {
        final RowDataset empty = dataset(List.of("k"), List.of());
        final Object key = new Object() {
            @Override public int hashCode() { throw new AssertionError("right row indexed for empty left"); }
        };
        final RowDataset right = dataset(List.of("k"), List.of(key));
        assertEquals(0, empty.intersect(right).size());
        assertEquals(0, empty.except(right).size());
        assertEquals(0, empty.intersectAll(right).size());
        assertEquals(0, empty.exceptAll(right).size());
        assertEquals(0, empty.semiJoin(right, List.of("k")).size());
        assertEquals(0, empty.antiJoin(right, List.of("k")).size());
        assertThrows(IllegalArgumentException.class, () -> empty.intersect(dataset(List.of("wrong"), List.of(1))));
    }

    @Test
    public void flatMapCanShrinkToEmptyWithoutInputSizedStorage() {
        final RowDataset data = dataset(List.of("k", "v"), Collections.nCopies(100000, 1), Collections.nCopies(100000, "x"));
        data.flatMapColumn("k", "mapped", List.of("v"), value -> List.of());
        final long bytes = allocated(() -> allocationSink = data.flatMapColumn("k", "mapped", List.of("v"), value -> List.of()));
        final Dataset empty = (Dataset) allocationSink;
        assertEquals(0, empty.size());
        assertEquals(List.of("v", "mapped"), empty.columnNames());
        assertTrue(bytes < 100000, "empty output allocated input-sized column storage: " + bytes);
        final Dataset expanded = dataset(List.of("k", "v"), List.of(1, 2), List.of("a", "b"))
                .flatMapColumn("k", "mapped", List.of("v"), value -> Arrays.asList(value, null));
        assertEquals(Arrays.asList(1, null, 2, null), expanded.getColumn("mapped"));
        assertEquals(List.of("a", "a", "b", "b"), expanded.getColumn("v"));
    }

    @Test
    public void csvImporterAdoptsPrivateColumnsButKeepsCallbackHeadersIsolated() {
        final AtomicReference<List<String>> headers = new AtomicReference<>();
        final Dataset data = CsvUtil.load(new StringReader("a,b\n1,2\n3,4\n"), null, 0, Long.MAX_VALUE, row -> true,
                (names, row, output) -> {
                    headers.set(names);
                    output[0] = row.get(0); output[1] = row.get(1);
                });
        assertEquals(2, data.size());
        data.renameColumn("a", "renamed");
        assertEquals(List.of("a", "b"), headers.get());
        final List<Object> values = new ArrayList<>(List.of(1, 2));
        final RowDataset copied = new RowDataset(new ArrayList<>(List.of("k")), new ArrayList<>(List.of(values)));
        values.set(0, 99);
        assertEquals(Integer.valueOf(1), copied.get(0, 0));
    }

    @SafeVarargs
    private static RowDataset dataset(List<String> names, List<?>... columns) {
        final List<List<Object>> copied = new ArrayList<>();
        for (List<?> column : columns) { copied.add(new ArrayList<>(column)); }
        return new RowDataset(new ArrayList<>(names), copied);
    }

    private static void combineFinite(int count) {
        final KahanSummation sum = new KahanSummation();
        final KahanSummation other = KahanSummation.of(0.1);
        for (int i = 0; i < count; i++) { sum.combine(1, 0.1); sum.combine(other); }
        allocationSink = sum;
    }

    private static long allocated(Runnable action) {
        final java.lang.management.ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        org.junit.jupiter.api.Assumptions.assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean allocation = (com.sun.management.ThreadMXBean) bean;
        org.junit.jupiter.api.Assumptions.assumeTrue(allocation.isThreadAllocatedMemorySupported());
        if (!allocation.isThreadAllocatedMemoryEnabled()) { allocation.setThreadAllocatedMemoryEnabled(true); }
        final long id = Thread.currentThread().threadId();
        final long before = allocation.getThreadAllocatedBytes(id);
        action.run();
        return allocation.getThreadAllocatedBytes(id) - before;
    }

    private static Object field(Object object, String name) throws Exception {
        for (Class<?> type = object.getClass(); type != null; type = type.getSuperclass()) {
            try {
                final Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException ignored) {
                // Try inherited storage.
            }
        }
        throw new NoSuchFieldException(name);
    }
}
