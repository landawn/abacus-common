package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collector;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlDeserConfig;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.function.BiObjIntFunction;
import com.landawn.abacus.util.function.BooleanToCharFunction;
import com.landawn.abacus.util.function.BooleanToIntFunction;
import com.landawn.abacus.util.function.BooleanTriFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongNConsumer;
import com.landawn.abacus.util.function.ObjBooleanConsumer;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.ObjCharConsumer;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.ObjLongFunction;
import com.landawn.abacus.util.function.ObjLongPredicate;
import com.landawn.abacus.util.function.ObjShortConsumer;
import com.landawn.abacus.util.function.QuadConsumer;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.FloatStream;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ShortStream;
import com.landawn.abacus.util.stream.Stream;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

@Tag("unit")
class InstructionCoverageLiftTest extends TestBase {

    @Test
    void digestOfFileAndPathMatchesTheByteOverload() throws Exception {
        final byte[] data = "coverage-digest".getBytes(StandardCharsets.UTF_8);
        final Path path = Files.createTempFile("cov-digest", ".bin");

        try {
            Files.write(path, data);
            final java.io.File file = path.toFile();

            assertArrayEquals(DigestUtil.md5(data), DigestUtil.md5(file));
            assertArrayEquals(DigestUtil.md5(data), DigestUtil.md5(path, StandardOpenOption.READ));
            assertEquals(DigestUtil.md5Hex(data), DigestUtil.md5Hex(file));
            assertEquals(DigestUtil.md5Hex(data), DigestUtil.md5Hex(path));
            assertEquals(DigestUtil.md5Hex(data), DigestUtil.md5Hex(path, StandardOpenOption.READ));
            assertArrayEquals(DigestUtil.sha256(data), DigestUtil.sha256(file));
            assertArrayEquals(DigestUtil.sha256(data), DigestUtil.sha256(path, StandardOpenOption.READ));
            assertEquals(DigestUtil.sha256Hex(data), DigestUtil.sha256Hex(file));
            assertEquals(DigestUtil.sha256Hex(data), DigestUtil.sha256Hex(path));
            assertEquals(DigestUtil.sha256Hex(data), DigestUtil.sha256Hex(path, StandardOpenOption.READ));
            assertArrayEquals(DigestUtil.sha512(data), DigestUtil.sha512(file));
            assertArrayEquals(DigestUtil.sha512(data), DigestUtil.sha512(path));
            assertArrayEquals(DigestUtil.sha512(data), DigestUtil.sha512(path, StandardOpenOption.READ));
            assertEquals(DigestUtil.sha512Hex(data), DigestUtil.sha512Hex(file));
            assertEquals(DigestUtil.sha512Hex(data), DigestUtil.sha512Hex(path));
            assertEquals(DigestUtil.sha512Hex(data), DigestUtil.sha512Hex(path, StandardOpenOption.READ));

            try (InputStream input = new ByteArrayInputStream(data)) {
                assertArrayEquals(DigestUtil.sha224(data), DigestUtil.sha224(input));
            }
            try (InputStream input = new ByteArrayInputStream(data)) {
                assertEquals(DigestUtil.sha224Hex(data), DigestUtil.sha224Hex(input));
            }
            assertEquals(DigestUtil.md5Hex("coverage-digest"), DigestUtil.md5Hex(file));
        } finally {
            Files.deleteIfExists(path);
        }
    }

    @Test
    void jsonPrettyFormatAndXmlRoundTripUseTheUncoveredOverloads() throws Exception {
        final String pretty = N.formatJson("{\"a\":1}", new JsonSerConfig(), Map.class);
        assertTrue(pretty.contains("\n"), pretty);
        assertTrue(pretty.contains("a"), pretty);

        final MergeBean bean = new MergeBean();
        bean.setId(7);
        bean.setName("Ada");
        final ByteArrayOutputStream xmlOut = new ByteArrayOutputStream();
        N.toXml(bean, new XmlSerConfig(), xmlOut);
        final String xml = xmlOut.toString(StandardCharsets.UTF_8);
        assertTrue(xml.contains("Ada"), xml);

        final MergeBean fallback = new MergeBean();
        fallback.setName("fallback");
        final Type<MergeBean> type = TypeFactory.getType(MergeBean.class);
        final MergeBean parsed = N.fromXml(xml, fallback, type);
        assertEquals("Ada", parsed.getName(), xml);
        assertEquals(Integer.valueOf(7), parsed.getId());

        final Path xmlFile = Files.createTempFile("cov-xml", ".xml");
        try {
            Files.writeString(xmlFile, xml);
            assertEquals("Ada", N.fromXml(xmlFile.toFile(), new XmlDeserConfig(), MergeBean.class).getName());

            try (InputStream input = Files.newInputStream(xmlFile)) {
                assertEquals(Integer.valueOf(7), N.fromXml(input, new XmlDeserConfig(), MergeBean.class).getId());
            }
            try (Reader reader = Files.newBufferedReader(xmlFile)) {
                assertEquals("Ada", N.fromXml(reader, new XmlDeserConfig(), MergeBean.class).getName());
            }
        } finally {
            Files.deleteIfExists(xmlFile);
        }
    }

    @Test
    void datesTimeZoneOverloadsAndLongAmounts() {
        final TimeZone utc = TimeZone.getTimeZone("UTC");
        final Date epoch = new Date(0L);
        final Date nextDay = new Date(86_400_000L);
        assertTrue(Dates.isSameYear(epoch, nextDay, utc));
        assertTrue(Dates.isSameMonth(epoch, nextDay, utc));

        final Calendar first = Calendar.getInstance(utc);
        first.setTimeInMillis(0L);
        final Calendar second = Calendar.getInstance(utc);
        second.setTimeInMillis(86_400_000L);
        assertTrue(Dates.isSameYear(first, second, utc));
        assertTrue(Dates.isSameMonth(first, second, utc));
        assertTrue(Dates.isSameYear(epoch, nextDay, ZoneId.of("UTC")));
        assertTrue(Dates.isSameMonth(first, second, ZoneId.of("UTC")));

        assertEquals(2L * 3_600_000L, Dates.addHours(new Date(0L), 2L).getTime());
        assertEquals(2L * 60_000L, Dates.addMinutes(new Date(0L), 2L).getTime());
        assertEquals(2_000L, Dates.addSeconds(new Date(0L), 2L).getTime());

        final Calendar hours = Calendar.getInstance(utc);
        hours.setTimeInMillis(0L);
        assertEquals(3_600_000L, Dates.addHours(hours, 1L).getTimeInMillis());
        final Calendar minutes = Calendar.getInstance(utc);
        minutes.setTimeInMillis(0L);
        assertEquals(60_000L, Dates.addMinutes(minutes, 1L).getTimeInMillis());
        final Calendar seconds = Calendar.getInstance(utc);
        seconds.setTimeInMillis(0L);
        assertEquals(1_000L, Dates.addSeconds(seconds, 1L).getTimeInMillis());

        final XMLGregorianCalendar calendar = Dates.currentXMLGregorianCalendarPlus(1L, TimeUnit.HOURS);
        assertNotNull(calendar);
        assertNotNull(Dates.currentXMLGregorianCalendarPlus(0L, TimeUnit.MILLISECONDS));
    }

    @Test
    void indexOfSubArrayFromAnOffset() {
        assertEquals(1, Index.ofSubArray(new boolean[] { false, true, false }, 0, new boolean[] { true, false }).orElse(-1));
        assertFalse(Index.ofSubArray(new boolean[] { false, true }, 1, new boolean[] { false }).isPresent());
        assertEquals(0, Index.ofSubArray(new boolean[] { true, false }, 0, new boolean[0]).orElse(-1));

        assertEquals(1, Index.ofSubArray(new byte[] { 1, 2, 3 }, 1, new byte[] { 2, 3 }).orElse(-1));
        assertFalse(Index.ofSubArray(new byte[] { 1, 2 }, 1, new byte[] { 1 }).isPresent());
        assertEquals(0, Index.ofSubArray(new byte[] { 9 }, 0, new byte[0]).orElse(-1));

        assertEquals(1, Index.ofSubArray(new short[] { 1, 2, 3 }, 0, new short[] { 2, 3 }).orElse(-1));
        assertEquals(1, Index.ofSubArray(new long[] { 1L, 2L, 3L }, 0, new long[] { 2L, 3L }).orElse(-1));
        assertEquals(1, Index.ofSubArray(new float[] { 1F, 2F, 3F }, 0, new float[] { 2F, 3F }).orElse(-1));
        assertEquals(1, Index.ofSubArray(new double[] { 1D, 2D, 3D }, 0, new double[] { 2D, 3D }).orElse(-1));
        assertEquals(1, Index.ofSubArray(new String[] { "a", "b", "c" }, 0, new String[] { "b", "c" }).orElse(-1));
        assertFalse(Index.ofSubArray(new String[] { "a", "b" }, 1, new String[] { "a" }).isPresent());
        assertEquals(2, Index.ofSubArray(new String[] { "a", "b" }, 5, new String[0]).orElse(-1));
    }

    @Test
    void readersWritersMovesDiskSpaceAndOptionalSymlink() throws Exception {
        final Path dir = Files.createTempDirectory("cov-io");

        try {
            final Path file = dir.resolve("note.txt");
            try (BufferedWriter writer = IOUtil.newBufferedWriter(file.toString())) {
                writer.write("hello");
            }
            try (BufferedReader reader = IOUtil.newBufferedReader(file.toString())) {
                assertEquals("hello", reader.readLine());
            }

            final Path moveSrc = dir.resolve("move-me.txt");
            final Path moveDir = dir.resolve("dest");
            Files.writeString(moveSrc, "moved");
            IOUtil.move(moveSrc.toFile(), moveDir.toFile());
            assertEquals("moved", Files.readString(moveDir.resolve("move-me.txt")));

            final Path moveSrc2 = dir.resolve("move-opt.txt");
            Files.writeString(moveSrc2, "opt");
            IOUtil.move(moveSrc2.toFile(), moveDir.toFile(), StandardCopyOption.REPLACE_EXISTING);
            assertEquals("opt", Files.readString(moveDir.resolve("move-opt.txt")));

            final Path moveSrc3 = dir.resolve("move-path.txt");
            final Path movedPath = dir.resolve("renamed.txt");
            Files.writeString(moveSrc3, "path");
            IOUtil.move(moveSrc3, movedPath);
            assertEquals("path", Files.readString(movedPath));

            try {
                final long free = IOUtil.freeDiskSpaceInKB();
                assertTrue(free > 0L);
                assertTrue(IOUtil.freeDiskSpaceInKB(5_000L) > 0L);
                final String here = new java.io.File(".").getAbsolutePath();
                assertTrue(IOUtil.freeDiskSpaceInKB(here) > 0L);
                assertTrue(IOUtil.freeDiskSpaceInKB(here, 5_000L) > 0L);
            } catch (final IllegalStateException | UncheckedIOException ex) {
                assertNotNull(ex.getMessage());
            }

            final Path linkDir = dir.resolve("links");
            final Path linkDest = dir.resolve("link-out");
            Files.createDirectories(linkDir);
            final Path target = linkDir.resolve("target.txt");
            Files.writeString(target, "link");
            try {
                Files.createSymbolicLink(linkDir.resolve("shortcut.txt"), Path.of("target.txt"));
                IOUtil.copyDirectory(linkDir.toFile(), linkDest.toFile());
                assertTrue(Files.exists(linkDest.resolve("target.txt")));
            } catch (final UnsupportedOperationException | IOException ex) {
                assertNotNull(ex.getMessage());
            }
        } finally {
            deleteTree(dir);
        }
    }

    @Test
    void fileSystemParserAndUnixProbe() throws Exception {
        final FileSystemUtil fs = fileSystem();

        assertEquals(1_234L, fs.parseDir("  1,234 bytes free", "C:\\"));
        assertEquals(1_2345L, fs.parseDir("prefix 12.345 suffix", "C:\\"));
        assertEquals(42L, fs.parseDir("42", "C:\\"));
        assertThrows(IOException.class, () -> fs.parseDir("no digits here", "C:\\"));
        assertThrows(IOException.class, () -> fs.parseBytes("-1", "C:\\"));
        assertThrows(IOException.class, () -> fs.parseBytes("nope", "C:\\"));
        assertEquals(8L, fs.parseBytes("8", "C:\\"));

        for (final boolean kb : new boolean[] { false, true }) {
            for (final boolean posix : new boolean[] { false, true }) {
                try {
                    assertTrue(fs.freeSpaceUnix(".", kb, posix, 2_000L) >= 0L);
                } catch (final IOException ex) {
                    assertNotNull(ex.getMessage());
                }
            }
        }
        assertThrows(IllegalArgumentException.class, () -> fs.freeSpaceUnix("", true, false, 100L));
    }

    @Test
    void commonUtilFactoriesAndOrdering() {
        final List<String> seven = CommonUtil.asList("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, seven.size());
        assertEquals("g", seven.get(6));
        assertEquals(Integer.valueOf(1), CommonUtil.toLinkedHashMap("a", 1).get("a"));
        assertEquals("", CommonUtil.nullToEmpty((String) null));
        assertTrue(CommonUtil.lt("a", "b"));
        assertTrue(CommonUtil.lt("a", "b", Comparator.naturalOrder()));
        assertFalse(CommonUtil.lt("b", "a", Comparator.naturalOrder()));
        assertTrue(CommonUtil.gt("b", "a"));
    }

    @Test
    void binaryOperatorsApplyBothSizeDirections() {
        final List<String> small = new ArrayList<>(List.of("a"));
        final List<String> big = new ArrayList<>(List.of("b", "c"));
        assertSame(big, BinaryOperators.<String, List<String>> ofAddAllToBigger().apply(small, big));
        assertEquals(List.of("b", "c", "a"), big);
        final List<String> left = new ArrayList<>(List.of("a", "b"));
        final List<String> right = new ArrayList<>(List.of("c"));
        assertSame(left, BinaryOperators.<String, List<String>> ofAddAllToBigger().apply(left, right));
        assertEquals(List.of("a", "b", "c"), left);

        final Map<String, String> smallMap = new LinkedHashMap<>();
        smallMap.put("a", "1");
        final Map<String, String> bigMap = new LinkedHashMap<>();
        bigMap.put("b", "2");
        bigMap.put("c", "3");
        assertSame(bigMap, BinaryOperators.<String, String, Map<String, String>> ofPutAllToBigger().apply(smallMap, bigMap));
        assertEquals("1", bigMap.get("a"));
        final Map<String, String> firstMap = new LinkedHashMap<>();
        firstMap.put("a", "1");
        firstMap.put("b", "2");
        final Map<String, String> secondMap = new LinkedHashMap<>();
        secondMap.put("c", "3");
        assertSame(firstMap, BinaryOperators.<String, String, Map<String, String>> ofPutAllToBigger().apply(firstMap, secondMap));
        assertEquals("3", firstMap.get("c"));

        final StringBuilder inserted = BinaryOperators.ofAppendToBigger().apply(new StringBuilder("a"), new StringBuilder("bbb"));
        assertEquals("abbb", inserted.toString());
        final StringBuilder appended = BinaryOperators.ofAppendToBigger().apply(new StringBuilder("cccc"), new StringBuilder("d"));
        assertEquals("ccccd", appended.toString());

        final Joiner shorter = Joiner.with(",").append("b");
        final Joiner longer = Joiner.with(",").append("aaaa");
        final Joiner intoLonger = BinaryOperators.ofMergeToBigger().apply(shorter, longer);
        assertSame(longer, intoLonger);
        assertTrue(intoLonger.length() > 4);
        final Joiner back = BinaryOperators.ofMergeToBigger().apply(Joiner.with(",").append("zzzz"), Joiner.with(",").append("y"));
        assertTrue(back.length() > 4);

        assertEquals("ab", BinaryOperators.ofConcat().apply("a", "b"));
        assertEquals(Integer.valueOf(5), BinaryOperators.ofAddInt().apply(2, 3));
        assertEquals(Long.valueOf(5L), BinaryOperators.ofAddLong().apply(2L, 3L));
        assertEquals(Double.valueOf(5D), BinaryOperators.ofAddDouble().apply(2D, 3D));
        assertEquals(BigInteger.valueOf(5L), BinaryOperators.ofAddBigInteger().apply(BigInteger.TWO, BigInteger.valueOf(3L)));
        assertEquals(0, new BigDecimal("5.0").compareTo(BinaryOperators.ofAddBigDecimal().apply(new BigDecimal("2.0"), new BigDecimal("3.0"))));

        final List<String> added = new ArrayList<>(List.of("a"));
        assertSame(added, BinaryOperators.<String, List<String>> ofAddAll().apply(added, new ArrayList<>(List.of("b"))));
        assertEquals(List.of("a"),
                BinaryOperators.<String, List<String>> ofRemoveAll().apply(new ArrayList<>(List.of("a", "b")), new ArrayList<>(List.of("b"))));
        final Map<String, String> put = new LinkedHashMap<>();
        put.put("a", "1");
        assertEquals("2", BinaryOperators.<String, String, Map<String, String>> ofPutAll().apply(put, Map.of("b", "2")).get("b"));
        assertEquals("ab", BinaryOperators.ofAppend().apply(new StringBuilder("a"), new StringBuilder("b")).toString());
        assertTrue(BinaryOperators.ofMerge().apply(Joiner.with(",").append("a"), Joiner.with(",").append("b")).length() > 1);
    }

    @Test
    void biFunctionsAndConsumersRunTheirActions() {
        assertEquals("a", BiFunctions.<String, Integer> selectFirst().apply("a", 1));
        assertEquals(Integer.valueOf(1), BiFunctions.<String, Integer> selectSecond().apply("a", 1));

        final List<String> values = new ArrayList<>();
        assertSame(values, BiFunctions.<String, List<String>> ofAdd().apply(values, "a"));
        final List<String> more = new ArrayList<>(List.of("b"));
        assertSame(values, BiFunctions.<String, List<String>> ofAddAll().apply(values, more));
        assertEquals(List.of("a", "b"), values);
        assertSame(values, BiFunctions.<String, List<String>> ofRemove().apply(values, "a"));
        assertSame(values, BiFunctions.<String, List<String>> ofRemoveAll().apply(values, more));
        assertTrue(values.isEmpty());

        final Map<String, Integer> map = new LinkedHashMap<>();
        BiFunctions.<String, Integer, Map<String, Integer>, Map.Entry<String, Integer>> ofPut().apply(map, Map.entry("a", 1));
        final Map<String, Integer> incoming = new LinkedHashMap<>();
        incoming.put("b", 2);
        BiFunctions.<String, Integer, Map<String, Integer>> ofPutAll().apply(map, incoming);
        BiFunctions.<String, Integer, Map<String, Integer>> ofRemoveByKey().apply(map, "a");
        assertFalse(map.containsKey("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));

        final List<String> consumed = new ArrayList<>();
        BiConsumers.<String, List<String>> ofAdd().accept(consumed, "a");
        BiConsumers.<String, List<String>> ofAddAll().accept(consumed, List.of("b"));
        BiConsumers.<String, List<String>> ofRemove().accept(consumed, "a");
        BiConsumers.<String, List<String>> ofRemoveAll().accept(consumed, new ArrayList<>(List.of("b")));
        assertTrue(consumed.isEmpty());

        final Map<String, Integer> consumedMap = new LinkedHashMap<>();
        BiConsumers.<String, Integer, Map<String, Integer>, Map.Entry<String, Integer>> ofPut().accept(consumedMap, Map.entry("a", 1));
        BiConsumers.<String, Integer, Map<String, Integer>> ofPutAll().accept(consumedMap, incoming);
        BiConsumers.<String, Integer, Map<String, Integer>> ofRemoveByKey().accept(consumedMap, "a");
        assertEquals(Integer.valueOf(2), consumedMap.get("b"));

        final List<String> indexed = new ArrayList<>();
        BiConsumers.<String, Integer> indexed((index, left, right) -> indexed.add(index + ":" + left + right)).accept("x", 1);
        assertEquals(List.of("0:x1"), indexed);
        BiConsumers.<String, Integer> doNothing().accept("x", 1);
        BiConsumers.ofMerge().accept(Joiner.with(",").append("a"), Joiner.with(",").append("b"));
        final StringBuilder appended = new StringBuilder("a");
        BiConsumers.ofAppend().accept(appended, "b");
        assertEquals("ab", appended.toString());
    }

    @Test
    void predicatesPartialApplicationAndAdapters() throws Exception {
        assertTrue(TriPredicates.alwaysTrue().test(1, 2, 3));
        assertFalse(TriPredicates.alwaysFalse().test(1, 2, 3));

        assertEquals(Integer.valueOf(1), Fn.compareTo(0, Comparator.<Integer> naturalOrder()).apply(5));
        assertTrue(Fn.p("ab", (String text, Integer start, Integer end) -> text.substring(start, end).equals("a")).test(0, 1));
        assertTrue(Fn.p((String a, String b, String c) -> (a + b).equals(c)).test("a", "b", "ab"));

        final List<String> log = new ArrayList<>();
        Fn.c("p", "s", (String prefix, String suffix, String message) -> log.add(prefix + message + suffix)).accept("m");
        Fn.c((String left, String right) -> log.add(left + right)).accept("a", "b");
        Fn.c("[", (String prefix, String left, String right) -> log.add(prefix + left + right)).accept("x", "y");
        Fn.c((String a, String b, String c) -> log.add(a + b + c)).accept("x", "y", "z");
        assertEquals("Hello, World", Fn.f("Hello, ", (String left, String right) -> left + right).apply("World"));
        assertEquals("abc", Fn.f("a", (String x, String y, String z) -> x + y + z).apply("b", "c"));
        assertEquals("done", Fn.c2f((java.util.function.Consumer<String>) log::add, "done").apply("x"));
        assertNull(Fn.c2f((String text, Integer number) -> log.add(text + number)).apply("n", 1));
        assertEquals("ok", Fn.c2f((String text, Integer number) -> log.add(text + number), "ok").apply("n", 2));
        assertNull(Fn.c2f((String a, String b, String c) -> log.add(a + b + c)).apply("p", "q", "r"));
        Fn.f2c((String text) -> text.length()).accept("hi");
        Fn.f2c((String left, String right) -> left + right).accept("a", "b");
        assertEquals("result", Fn.c(() -> "result").call());
        assertTrue(log.contains("pms"));
    }

    @Test
    void functionCompositionAndBooleanConversions() {
        final List<String> order = new ArrayList<>();
        final TriConsumer<String, String, String> tri = (a, b, c) -> order.add(a);
        tri.andThen((a, b, c) -> order.add(c)).accept("1", "2", "3");
        final QuadConsumer<String, String, String, String> quad = (a, b, c, d) -> order.add(b);
        quad.andThen((a, b, c, d) -> order.add(d)).accept("a", "b", "c", "d");

        final ObjShortConsumer<List<String>> shorts = (sink, value) -> sink.add(Short.toString(value));
        shorts.andThen((sink, value) -> sink.add("s")).accept(order, (short) 4);
        final ObjFloatConsumer<List<String>> floats = (sink, value) -> sink.add(Float.toString(value));
        floats.andThen((sink, value) -> sink.add("f")).accept(order, 1F);
        final ObjCharConsumer<List<String>> chars = (sink, value) -> sink.add(String.valueOf(value));
        chars.andThen((sink, value) -> sink.add("c")).accept(order, 'q');
        final ObjByteConsumer<List<String>> bytes = (sink, value) -> sink.add(Byte.toString(value));
        bytes.andThen((sink, value) -> sink.add("y")).accept(order, (byte) 8);
        final ObjBooleanConsumer<List<String>> flags = (sink, value) -> sink.add(Boolean.toString(value));
        flags.andThen((sink, value) -> sink.add("z")).accept(order, true);

        final LongConsumer longs = value -> order.add(Long.toString(value));
        longs.andThen(value -> order.add("L")).accept(9L);
        final LongNConsumer many = args -> order.add(String.valueOf(args.length));
        many.andThen(args -> order.add("N")).accept(1L, 2L);

        final TriFunction<String, String, String, String> join = (a, b, c) -> a + b + c;
        assertEquals(3, join.andThen(String::length).apply("a", "b", "c"));
        final QuadFunction<String, String, String, String, String> join4 = (a, b, c, d) -> a + b + c + d;
        assertEquals(4, join4.andThen(String::length).apply("a", "b", "c", "d"));
        final ObjLongFunction<String, String> objLong = (text, value) -> text + value;
        assertEquals(2, objLong.andThen(String::length).apply("a", 7L));
        final DoubleTriFunction<Double> sum = (a, b, c) -> a + b + c;
        assertEquals(6D, sum.andThen(value -> value).apply(1D, 2D, 3D), 0D);
        final BooleanTriFunction<String> bits = (a, b, c) -> a + "," + b + "," + c;
        assertEquals("true,false,true", bits.andThen(value -> value).apply(true, false, true));
        final BiObjIntFunction<String, String, Integer> width = (a, b, n) -> a.length() + b.length() + n;
        assertEquals("5", width.andThen(Object::toString).apply("a", "bb", 2));

        final ObjLongPredicate<String> longer = (text, value) -> text.length() > value;
        assertFalse(longer.negate().test("ab", 1L));
        assertTrue(longer.negate().test("ab", 5L));
        assertEquals(1, BooleanToIntFunction.DEFAULT.applyAsInt(true));
        assertEquals(0, BooleanToIntFunction.DEFAULT.applyAsInt(false));
        assertEquals('Y', BooleanToCharFunction.DEFAULT.applyAsChar(true));
        assertEquals('N', BooleanToCharFunction.DEFAULT.applyAsChar(false));
        assertTrue(order.contains("1"));
        assertTrue(order.contains("3"));
    }

    @Test
    void strUtilSubstringOverloads() {
        final IntUnaryOperator plusFive = index -> index + 5;

        assertEquals("hello", Strings.StrUtil.substring("hello", 0, plusFive).orElse(null));
        assertFalse(Strings.StrUtil.substring("hello", 6, plusFive).isPresent());
        assertFalse(Strings.StrUtil.substring(null, 0, plusFive).isPresent());
        assertEquals("hello", Strings.StrUtil.substring("hello", end -> 0, 5).orElse(null));
        assertEquals("hello", Strings.StrUtil.substringOrElse("hello", 0, plusFive, "default"));
        assertEquals("default", Strings.StrUtil.substringOrElse("hello", end -> 10, 3, "default"));
        assertEquals("default", Strings.StrUtil.substringOrElse(null, end -> 0, 5, "default"));
        assertEquals("hello", Strings.StrUtil.substringOrElseItself("hello", 0, plusFive));
        assertEquals("hello", Strings.StrUtil.substringOrElseItself("hello", end -> 10, 3));
        assertNull(Strings.StrUtil.substringOrElseItself(null, 0, plusFive));

        assertEquals("hello", Strings.StrUtil.substringBetween("*hello*", '*', '*').orElse(null));
        assertEquals("hello", Strings.StrUtil.substringBetween("[hello]", 0, ']').orElse(null));
        assertEquals("World", Strings.StrUtil.substringBetween("Hello World Java", 5, " ").orElse(null));
        assertEquals("hello", Strings.StrUtil.substringBetween("*hello*world*", "*").orElse(null));
        assertFalse(Strings.StrUtil.substringBetween("hello world", "*").isPresent());
        assertEquals("hello", Strings.StrUtil.substringBetween("a*hello*b", 0, "*", "*").orElse(null));
        assertEquals("hell", Strings.StrUtil.substringBetween("hello", end -> -1, 4).orElse(null));
        assertEquals("lo", Strings.StrUtil.substring("hello", 3, index -> index + 2).orElse(null));
    }

    @Test
    void collectorCombinersAndFlatMappings() {
        final BooleanList booleans = combineTwo(Collectors.toBooleanList(), Boolean.TRUE, Boolean.FALSE);
        assertEquals(2, booleans.size());
        final CharList chars = combineTwo(Collectors.toCharList(), 'a', 'b');
        assertEquals(2, chars.size());
        assertArrayEquals(new char[] { 'a', 'b' }, combineTwo(Collectors.toCharArray(), 'a', 'b'));

        assertEquals(List.of("only"), combineWithEmpty(Collectors.first(2), "only"));
        assertThrows(UnsupportedOperationException.class, () -> combineTwo(Collectors.first(2), "a", "b"));
        assertNotNull(combineEmpty(Collectors.first(2)));

        assertEquals(3L, combineTwo(Collectors.summarizingInt(String::length), "ab", "c").getSum());
        assertEquals(3L, combineTwo(Collectors.summarizingLong(String::length), "ab", "c").getSum());
        assertEquals(3D, combineTwo(Collectors.summarizingDouble(String::length), "ab", "c").getSum(), 0D);
        assertEquals(2L, combineTwo(Collectors.summarizingByte((String text) -> (byte) text.length()), "ab", "c").getCount());
        assertEquals(0,
                BigInteger.valueOf(3L).compareTo(combineTwo(Collectors.summarizingBigInteger(text -> BigInteger.valueOf(text.length())), "ab", "c").getSum()));
        assertEquals(0,
                BigDecimal.valueOf(3L).compareTo(combineTwo(Collectors.summarizingBigDecimal(text -> BigDecimal.valueOf(text.length())), "ab", "c").getSum()));
        assertEquals(Integer.valueOf(3), combineTwo(Collectors.reducing(0, String::length, Integer::sum), "ab", "c"));

        final ListMultimap<Integer, String> values = combineTwo(
                Collectors.flatMappingValueToMultimap(String::length, text -> "a".equals(text) ? null : Stream.of(text, text + "!")), "a", "bb");
        assertTrue(values.get(2).contains("bb"));
        assertTrue(values.get(2).contains("bb!"));
        final ListMultimap<Integer, String> keys = combineTwo(
                Collectors.<String, Integer, String> flatMappingKeyToMultimap(text -> Stream.of(text.length(), text.length() + 1), text -> text), "a", "bb");
        assertTrue(keys.get(2).contains("bb"));
        assertTrue(keys.get(3).contains("bb"));
    }

    @Test
    void rowDatasetMergeMultimapAndXml() throws Exception {
        final Dataset data = Dataset.rows(List.of("id", "name"), new Object[][] { { 1, "Ada" }, { 1, "Bea" }, { 2, "Cy" } });

        final List<MergeBean> byPrefix = data.toMergedEntities(Map.of(), MergeBean.class);
        assertEquals(2, byPrefix.size());
        assertEquals(Integer.valueOf(1), byPrefix.get(0).getId());
        final List<MergeBean> byNamedPrefix = data.toMergedEntities("id", Map.of(), MergeBean.class);
        assertEquals(2, byNamedPrefix.size());
        final List<MergeBean> byIds = data.toMergedEntities(List.of("id"), Map.of(), MergeBean.class);
        assertEquals(2, byIds.size());

        final ListMultimap<Integer, MergeBean> rows = data.toMultimap("id", List.of("name"), MergeBean.class, len -> N.newLinkedListMultimap());
        assertEquals(2, rows.get(1).size());
        assertEquals("Ada", rows.get(1).get(0).getName());

        final StringWriter writer = new StringWriter();
        data.toXml(0, data.size(), List.of("id", "name"), writer);
        assertTrue(writer.toString().contains("Ada"), writer.toString());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        data.toXml(0, data.size(), List.of("id", "name"), out);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Bea"));
    }

    @Test
    void nullsFirstElementAndReversedComparator() {
        assertEquals("a", Nulls.firstElement(new String[] { "a", "b" }));
        assertNull(Nulls.firstElement(new String[] { null, "b" }));
        assertNull(Nulls.firstElement(new String[0]));
        assertNull(Nulls.firstElement((String[]) null));
        assertEquals("a", Nulls.firstElement(List.of("a", "b")));
        assertNull(Nulls.firstElement(Arrays.asList(null, "b")));
        assertNull(Nulls.firstElement(List.<String> of()));
        assertEquals("a", Nulls.firstElement(new LinkedHashSet<>(List.of("a", "b"))));
        assertEquals("a", Nulls.firstElement(List.of("a", "b").iterator()));
        assertNull(Nulls.firstElement(Arrays.asList(null, "b").iterator()));
        assertNull(Nulls.firstElement(Collections.emptyIterator()));
        assertNull(Nulls.firstElement((Iterator<String>) null));

        final Comparator<String> longestFirst = Comparators.reversedComparingBy(String::length, Comparator.naturalOrder());
        assertTrue(longestFirst.compare("bb", "a") < 0);
        assertTrue(longestFirst.compare("a", "bb") > 0);
        assertEquals(0, longestFirst.compare("aa", "bb"));
    }

    @Test
    void primitiveListBuildersInsertAtAnIndex() {
        final ShortList shorts = Builder.of(ShortList.of((short) 1, (short) 4)).addAll(1, ShortList.of((short) 2, (short) 3)).val();
        assertEquals((short) 2, shorts.get(1));
        assertEquals(4, shorts.size());
        final LongList longs = Builder.of(LongList.of(1L, 4L)).addAll(1, LongList.of(2L, 3L)).val();
        assertEquals(2L, longs.get(1));
        final FloatList floats = Builder.of(FloatList.of(1F, 4F)).addAll(1, FloatList.of(2F, 3F)).val();
        assertEquals(2F, floats.get(1), 0F);
        final DoubleList doubles = Builder.of(DoubleList.of(1D, 4D)).addAll(1, DoubleList.of(2D, 3D)).val();
        assertEquals(2D, doubles.get(1), 0D);
        final ByteList bytes = Builder.of(ByteList.of((byte) 1, (byte) 4)).addAll(1, ByteList.of((byte) 2, (byte) 3)).val();
        assertEquals((byte) 2, bytes.get(1));
    }

    @Test
    void methodHandleAndInvertMerge() throws Throwable {
        final Method length = String.class.getMethod("length");
        final MethodHandle handle = ClassUtil.createMethodHandle(length);
        assertEquals(Integer.valueOf(2), handle.invoke("ab"));

        final Map<String, String> source = new LinkedHashMap<>();
        source.put("key1", "valueA");
        source.put("key2", "valueA");
        source.put("key3", "valueB");
        final Map<String, String> inverted = Maps.invert(source, (oldKey, newKey) -> oldKey + "," + newKey);
        assertEquals("key1,key2", inverted.get("valueA"));
        assertEquals("key3", inverted.get("valueB"));
        final Map<String, String> dropped = Maps.invert(source, (oldKey, newKey) -> null);
        assertFalse(dropped.containsKey("valueA"));
        assertEquals("key3", dropped.get("valueB"));
    }

    @Test
    void numbersNarrowThroughTheSharedRangeCheck() {
        assertEquals((byte) 12, Numbers.toByte(BigInteger.valueOf(12L), (byte) 0));
        assertEquals((short) 12, Numbers.toShort(BigInteger.valueOf(12L), (short) 0));
        assertEquals(12, Numbers.toInt(BigInteger.valueOf(12L), 0));
        assertEquals(12L, Numbers.toLong(BigInteger.valueOf(12L), 0L));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(BigInteger.valueOf(200L), (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toShort(BigInteger.valueOf(40_000L), (short) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(BigInteger.valueOf(3_000_000_000L), 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(new BigInteger("9223372036854775808"), 0L));

        assertEquals(12, Numbers.toInt(new BigDecimal("12.9"), 0));
        assertEquals((byte) 12, Numbers.toByte(new BigDecimal("12.9"), (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(new BigDecimal("128.9"), (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(new BigDecimal("1e20"), 0));

        assertEquals(12, Numbers.toInt(Double.valueOf(12.9D), 0));
        assertEquals(12, Numbers.toInt(Float.valueOf(12.9F), 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(Double.NaN, 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toLong(Double.POSITIVE_INFINITY, 0L));
        assertEquals(12, Numbers.toInt(Byte.valueOf((byte) 12), 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toByte(Integer.valueOf(200), (byte) 0));
        assertThrows(ArithmeticException.class, () -> Numbers.toInt(Long.valueOf(Long.MAX_VALUE), 0));
        assertEquals(7, Numbers.toInt(null, 7));

        assertEquals(12, Numbers.toInt(new AtomicInteger(12), 0));
        assertEquals(12L, Numbers.toLong(new AtomicLong(12L), 0L));
        final LongAdder longAdder = new LongAdder();
        longAdder.add(12L);
        assertEquals(12L, Numbers.toLong(longAdder, 0L));
        final DoubleAdder doubleAdder = new DoubleAdder();
        doubleAdder.add(12.9D);
        assertEquals(12, Numbers.toInt(doubleAdder, 0));

        assertEquals(12, Numbers.toInt(new TextNumber("12.9", 99D), 0));
        assertEquals(16, Numbers.toInt(new TextNumber("0x10", 16D), 0));
        assertEquals(12, Numbers.toInt(new TextNumber(" 12", 12D), 0));
        assertEquals(9, Numbers.toInt(new TextNumber(null, 9D), 0));
    }

    @Test
    void typeDefaultsAndJsonValidate() {
        final Type<Integer> integers = TypeFactory.getType(Integer.class);
        final Type<String> strings = TypeFactory.getType(String.class);
        final Type<Object> objects = TypeFactory.getType(Object.class);

        assertFalse(integers.isString());
        assertFalse(integers.isCharSequence());
        assertFalse(integers.isDate());
        assertFalse(integers.isCalendar());
        assertFalse(integers.isJodaDateTime());
        assertFalse(integers.isTemporal());
        assertFalse(integers.isPrimitiveArray());
        assertFalse(integers.isPrimitiveByteArray());
        assertFalse(integers.isObjectArray());
        assertFalse(integers.isArray());
        assertFalse(integers.isList());
        assertFalse(integers.isSet());
        assertFalse(integers.isCollection());
        assertFalse(integers.isMap());
        assertFalse(integers.isBean());
        assertFalse(integers.isMapEntity());
        assertFalse(integers.isEntityId());
        assertFalse(integers.isDataset());
        assertFalse(integers.isInputStream());
        assertFalse(integers.isReader());
        assertFalse(integers.isByteBuffer());
        assertFalse(integers.isParameterizedType());
        assertTrue(integers.isImmutable());
        assertFalse(integers.isOptionalOrNullable());
        assertFalse(integers.isPrimitiveList());
        assertFalse(integers.isBoolean());
        assertTrue(integers.isNumber());
        assertTrue(integers.isPrimitiveWrapper());
        assertFalse(strings.isNumber());
        assertTrue(strings.isString());
        assertFalse(strings.isPrimitive());
        assertFalse(objects.isPrimitive());
        assertTrue(objects.isObject());
        assertFalse(objects.isBoolean());

        ParserFactory.createJsonParser().validate("{\"a\":1}");
        assertThrows(ParsingException.class, () -> ParserFactory.createJsonParser().validate("{\"a\":1} trailing"));
        assertThrows(ParsingException.class, () -> ParserFactory.createJsonParser().validate("{"));
    }

    @Test
    void seqBufferedQueueLeavesTheCallerQueueIntact() throws Exception {
        final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(4);
        final Seq<String, RuntimeException> buffered = Seq.<String, RuntimeException> of("a", "b").buffered(queue);

        try {
            assertEquals(List.of("a", "b"), buffered.toList());
        } finally {
            buffered.close();
        }
        assertTrue(queue.isEmpty());
    }

    @Test
    void entryStreamPartialAndFlatMap() {
        final Map<String, Integer> entries = new LinkedHashMap<>();
        entries.put("a", 1);
        entries.put("b", 2);

        assertEquals(1L, EntryStream.of(entries).mapKeyPartial(key -> "a".equals(key) ? Optional.of(key) : Optional.empty()).count());
        assertEquals(1L, EntryStream.of(entries).mapKeyPartial((key, value) -> value > 1 ? Optional.of(key) : Optional.empty()).count());
        assertEquals(1L, EntryStream.of(entries).mapValuePartial(value -> value > 1 ? Optional.of(value) : Optional.empty()).count());
        assertEquals(1L, EntryStream.of(entries).mapValuePartial((key, value) -> "b".equals(key) ? Optional.of(value + 1) : Optional.empty()).count());
        assertEquals(4L, EntryStream.of(entries).flatmapKey(key -> List.of(key, key + key)).count());
        assertEquals(2L, EntryStream.of(entries).flatmapKey((key, value) -> List.of(key + value)).count());
        assertEquals(4L, EntryStream.of(entries).flatmapValue(value -> List.of(value, value + 1)).count());
        assertEquals(2L, EntryStream.of(entries).flatmapValue((key, value) -> List.of(key + value)).count());
    }

    @Test
    void entityIdFromSelectedProperties() {
        final MergeBean bean = new MergeBean();
        bean.setId(4);
        bean.setName("Ada");

        final EntityId id = EntityId.create(bean, List.of("id"));

        assertEquals(4, id.getInt("id"));
        assertFalse(id.isEmpty());
    }

    @Test
    void streamGroupingZipDelayAndParallelTails() {
        final Map<Integer, List<String>> grouped = Stream.of("a", "bb", "a").groupTo(String::length, text -> text + "!", Collectors.toList());
        assertEquals(List.of("a!", "a!"), grouped.get(1));
        assertEquals(List.of("bb!"), grouped.get(2));
        assertTrue(Stream.of("a", "b", "c").combinations(2, true).skip(1).count() > 0L);
        assertTrue(Stream.of("a", "b").cartesianProduct(List.of(List.of("x", "y"))).skip(1).count() >= 0L);
        assertTrue(Stream.of("a", "b", "c").rollup().skip(1).count() > 0L);

        assertEquals(List.of("1a0.1", "2b0.2"), Stream.parallelZip(List.of(1, 2), List.of("a", "b"), List.of(0.1D, 0.2D), (n, s, d) -> n + s + d, 1).toList());
        assertEquals(List.of("a10.1"), Stream.parallelZip(Stream.of("a"), Stream.of(1), Stream.of(0.1D), (s, n, d) -> s + n + d, 1).toList());
        assertEquals(List.of("a1", "b0"), Stream.parallelZip(Stream.of("a", "b"), Stream.of(1), "?", 0, (s, n) -> s + n, 1).toList());
        assertEquals(List.of("a10.1", "?20.0"),
                Stream.parallelZip(Stream.of("a"), Stream.of(1, 2), Stream.of(0.1D), "?", 0, 0.0D, (s, n, d) -> s + n + d, 1).toList());

        assertEquals(List.of(1, 2), Stream.of(1, 2).delay(java.time.Duration.ofMillis(0)).toList());
        assertEquals(List.of(1), Stream.of(1).rateLimited(1_000_000D).toList());

        final AtomicInteger seen = new AtomicInteger();
        Stream.of("a", "b", "c").forEachIndexed((index, value) -> seen.incrementAndGet());
        Stream.of("a", "b", "c").parallel().forEachIndexed((index, value) -> seen.incrementAndGet());
        assertEquals(6, seen.get());
        assertTrue(Stream.of("a", "bb", "c").parallel().mapPartial(text -> text.length() > 1 ? Optional.of(text) : Optional.empty()).toList().contains("bb"));
        assertTrue(Stream.of("a", "bb")
                .parallel()
                .mapPartialJdk(text -> text.length() > 1 ? java.util.Optional.of(text) : java.util.Optional.empty())
                .toList()
                .contains("bb"));
        final List<String> debounced = Stream.of("a", "b", "c").parallel().debounce(Duration.ofMillis(1)).toList();
        assertFalse(debounced.isEmpty());
        assertTrue(debounced.size() <= 3);

        exerciseParallel(IntStream.of(1, 2, 3), IntStream.of(1, 2, 3).parallel());
        assertArrayEquals(new int[] { 4 },
                IntStream.of(1, 2).parallel().mapPartial(value -> value % 2 == 0 ? OptionalInt.of(value * 2) : OptionalInt.empty()).toArray());
        assertArrayEquals(new int[] { 6 },
                IntStream.of(1, 3)
                        .parallel()
                        .mapPartialJdk(value -> value > 2 ? java.util.OptionalInt.of(value * 2) : java.util.OptionalInt.empty())
                        .toArray());

        final AtomicInteger bytes = new AtomicInteger();
        ByteStream.of((byte) 1, (byte) 2).parallel().forEachIndexed((index, value) -> bytes.incrementAndGet());
        assertEquals(2, bytes.get());
        assertArrayEquals(new byte[] { 2 },
                ByteStream.of((byte) 1, (byte) 2).parallel().mapPartial(value -> value > 1 ? OptionalByte.of(value) : OptionalByte.empty()).toArray());
        assertTrue(ByteStream.of((byte) 1, (byte) 2, (byte) 3).parallel().debounce(Duration.ofMillis(1)).count() >= 1L);

        final AtomicInteger chars = new AtomicInteger();
        CharStream.of('a', 'b').parallel().forEachIndexed((index, value) -> chars.incrementAndGet());
        assertEquals(2, chars.get());
        assertArrayEquals(new char[] { 'b' },
                CharStream.of('a', 'b').parallel().mapPartial(value -> value == 'b' ? OptionalChar.of(value) : OptionalChar.empty()).toArray());
        assertTrue(CharStream.of('a', 'b', 'c').parallel().debounce(Duration.ofMillis(1)).count() >= 1L);

        final AtomicInteger shorts = new AtomicInteger();
        ShortStream.of((short) 1, (short) 2).parallel().forEachIndexed((index, value) -> shorts.incrementAndGet());
        assertEquals(2, shorts.get());
        assertArrayEquals(new short[] { 2 },
                ShortStream.of((short) 1, (short) 2).parallel().mapPartial(value -> value > 1 ? OptionalShort.of(value) : OptionalShort.empty()).toArray());
        assertTrue(ShortStream.of((short) 1, (short) 2, (short) 3).parallel().debounce(Duration.ofMillis(1)).count() >= 1L);

        final AtomicInteger longs = new AtomicInteger();
        LongStream.of(1L, 2L).parallel().forEachIndexed((index, value) -> longs.incrementAndGet());
        assertEquals(2, longs.get());
        assertArrayEquals(new long[] { 2L },
                LongStream.of(1L, 2L).parallel().mapPartial(value -> value > 1 ? OptionalLong.of(value) : OptionalLong.empty()).toArray());
        assertArrayEquals(new long[] { 6L },
                LongStream.of(1L, 3L)
                        .parallel()
                        .mapPartialJdk(value -> value > 2 ? java.util.OptionalLong.of(value * 2) : java.util.OptionalLong.empty())
                        .toArray());
        assertTrue(LongStream.of(1L, 2L, 3L).parallel().debounce(Duration.ofMillis(1)).count() >= 1L);

        final AtomicInteger floats = new AtomicInteger();
        FloatStream.of(1F, 2F).parallel().forEachIndexed((index, value) -> floats.incrementAndGet());
        assertEquals(2, floats.get());
        assertArrayEquals(new float[] { 2F },
                FloatStream.of(1F, 2F).parallel().mapPartial(value -> value > 1F ? OptionalFloat.of(value) : OptionalFloat.empty()).toArray(), 0F);
        assertTrue(FloatStream.of(1F, 2F, 3F).parallel().debounce(Duration.ofMillis(1)).count() >= 1L);

        final AtomicInteger doubles = new AtomicInteger();
        DoubleStream.of(1D, 2D).parallel().forEachIndexed((index, value) -> doubles.incrementAndGet());
        assertEquals(2, doubles.get());
        assertArrayEquals(new double[] { 2D },
                DoubleStream.of(1D, 2D).parallel().mapPartial(value -> value > 1D ? OptionalDouble.of(value) : OptionalDouble.empty()).toArray(), 0D);
        assertArrayEquals(new double[] { 6D },
                DoubleStream.of(1D, 3D)
                        .parallel()
                        .mapPartialJdk(value -> value > 2D ? java.util.OptionalDouble.of(value * 2D) : java.util.OptionalDouble.empty())
                        .toArray(),
                0D);
        assertTrue(DoubleStream.of(1D, 2D, 3D).parallel().debounce(Duration.ofMillis(1)).count() >= 1L);
    }

    @Test
    void summingAndAveragingCombinersMergeBothSides() {
        assertEquals(2, combineTwo(Collectors.toBooleanArray(), Boolean.TRUE, Boolean.FALSE).length);
        assertEquals(2, combineTwo(Collectors.toByteList(), (byte) 1, (byte) 2).size());
        assertEquals(2, combineTwo(Collectors.toShortList(), (short) 1, (short) 2).size());
        assertEquals(2, combineTwo(Collectors.toMultiset(), "a", "b").size());

        assertEquals(Integer.valueOf(3), combineTwo(Collectors.summingInt(String::length), "ab", "c"));
        assertEquals(Long.valueOf(3L), combineTwo(Collectors.summingIntToLong(String::length), "ab", "c"));
        assertEquals(Long.valueOf(3L), combineTwo(Collectors.summingLong(String::length), "ab", "c"));
        assertEquals(3D, combineTwo(Collectors.summingDouble(String::length), "ab", "c"), 0D);
        assertEquals(0, BigInteger.valueOf(3L).compareTo(combineTwo(Collectors.summingBigInteger(text -> BigInteger.valueOf(text.length())), "ab", "c")));
        assertEquals(0, BigDecimal.valueOf(3L).compareTo(combineTwo(Collectors.summingBigDecimal(text -> BigDecimal.valueOf(text.length())), "ab", "c")));

        final var summed = combineTwo(Collectors.MoreCollectors.summingInt(String::length, text -> text.length() + 1), "ab", "c");
        assertEquals(Integer.valueOf(3), summed._1);
        assertEquals(Integer.valueOf(5), summed._2);
        final var summedThree = combineTwo(Collectors.MoreCollectors.summingInt(String::length, String::length, String::length), "ab", "c");
        assertEquals(Integer.valueOf(3), summedThree._1);
        assertEquals(Integer.valueOf(3), summedThree._3);
        final var summedLong = combineTwo(Collectors.MoreCollectors.summingLong(String::length, String::length), "ab", "c");
        assertEquals(Long.valueOf(3L), summedLong._1);
        final var summedDouble = combineTwo(Collectors.MoreCollectors.summingDouble(String::length, String::length), "ab", "c");
        assertEquals(3D, summedDouble._1, 0D);
        final var summedBig = combineTwo(Collectors.MoreCollectors.summingBigInteger(text -> BigInteger.valueOf(text.length()), text -> BigInteger.ONE), "ab",
                "c");
        assertEquals(BigInteger.valueOf(3L), summedBig._1);
        final var summedDecimal = combineTwo(Collectors.MoreCollectors.summingBigDecimal(text -> BigDecimal.valueOf(text.length()), text -> BigDecimal.ONE),
                "ab", "c");
        assertEquals(0, BigDecimal.valueOf(3L).compareTo(summedDecimal._1));

        assertEquals(1.5D, combineTwo(Collectors.averagingInt(String::length), "ab", "c"), 0D);
        assertEquals(1.5D, combineTwo(Collectors.averagingLong(String::length), "ab", "c"), 0D);
        assertEquals(1.5D, combineTwo(Collectors.averagingDouble(String::length), "ab", "c"), 0D);
        assertTrue(combineTwo(Collectors.averagingIntOrEmpty(String::length), "ab", "c").isPresent());
        assertTrue(finishEmpty(Collectors.averagingIntOrEmpty(String::length)).isEmpty());
        assertThrows(NoSuchElementException.class, () -> finishEmpty(Collectors.averagingIntOrElseThrow(String::length)));
        assertThrows(NoSuchElementException.class, () -> finishEmpty(Collectors.averagingLongOrElseThrow(String::length)));
        assertThrows(NoSuchElementException.class, () -> finishEmpty(Collectors.averagingDoubleOrElseThrow(String::length)));
        assertEquals(0, new BigDecimal("2").compareTo(combineTwo(Collectors.averagingBigInteger(text -> BigInteger.valueOf(text.length())), "a", "bbb")));
        assertEquals(0, new BigDecimal("2").compareTo(combineTwo(Collectors.averagingBigDecimal(text -> BigDecimal.valueOf(text.length())), "a", "bbb")));
        assertThrows(NoSuchElementException.class, () -> finishEmpty(Collectors.averagingBigIntegerOrElseThrow(text -> BigInteger.ONE)));
        assertThrows(NoSuchElementException.class, () -> finishEmpty(Collectors.averagingBigDecimalOrElseThrow(text -> BigDecimal.ONE)));

        final var averaged = combineTwo(Collectors.MoreCollectors.averagingInt(String::length, String::length), "ab", "c");
        assertEquals(1.5D, averaged._1, 0D);
        final var averagedThree = combineTwo(Collectors.MoreCollectors.averagingLong(String::length, String::length, String::length), "ab", "c");
        assertEquals(1.5D, averagedThree._2, 0D);
        final var averagedDouble = combineTwo(Collectors.MoreCollectors.averagingDouble(String::length, String::length), "ab", "c");
        assertEquals(1.5D, averagedDouble._2, 0D);
        final var averagedBig = combineTwo(
                Collectors.MoreCollectors.averagingBigInteger(text -> BigInteger.valueOf(text.length()), text -> BigInteger.valueOf(text.length())), "a",
                "bbb");
        assertEquals(0, new BigDecimal("2").compareTo(averagedBig._1));
        final var averagedDecimal = combineTwo(
                Collectors.MoreCollectors.averagingBigDecimal(text -> BigDecimal.valueOf(text.length()), text -> BigDecimal.ONE, text -> BigDecimal.TEN), "a",
                "bbb");
        assertEquals(0, BigDecimal.ONE.compareTo(averagedDecimal._2));

        assertEquals(2L, combineTwo(Collectors.summarizingChar(text -> text.charAt(0)), "ab", "c").getCount());
        assertEquals(2L, combineTwo(Collectors.summarizingShort((String text) -> (short) text.length()), "ab", "c").getCount());
        assertEquals(2L, combineTwo(Collectors.summarizingFloat((String text) -> (float) text.length()), "ab", "c").getCount());
    }

    @Test
    void emptyRangesAndFlattenedRowsUseTheSharedEmptyArrays() {
        assertEquals(0, Array.range((byte) 2, (byte) 2, (byte) 1).length);
        assertEquals(0, Array.range((byte) 1, (byte) 4, (byte) -1).length);
        assertThrows(IllegalArgumentException.class, () -> Array.range((byte) 1, (byte) 4, (byte) 0));
        assertArrayEquals(new byte[] { 5 }, Array.rangeClosed((byte) 5, (byte) 5, (byte) 2));
        assertEquals(0, Array.rangeClosed((byte) 1, (byte) 4, (byte) -1).length);

        assertEquals(0, Array.range((short) 2, (short) 2, (short) 1).length);
        assertEquals(0, Array.rangeClosed((short) 1, (short) 4, (short) -1).length);
        assertEquals(1, Array.rangeClosed((short) 5, (short) 5, (short) 2).length);
        assertEquals(0, Array.range(2, 2, 1).length);
        assertEquals(0, Array.rangeClosed(1, 4, -1).length);
        assertEquals(1, Array.rangeClosed(5, 5, 3).length);
        assertEquals(0, Array.range(2L, 2L, 1L).length);
        assertEquals(0, Array.rangeClosed(1L, 4L, -1L).length);
        assertEquals(1, Array.rangeClosed(5L, 5L, 3L).length);
        assertEquals(0, Array.range('b', 'b', 1).length);
        assertThrows(IllegalArgumentException.class, () -> Array.range('a', 'c', 0));

        assertNull(Array.box((byte[]) null, 0, 0));
        assertEquals(0, Array.box(new byte[] { 1 }, 0, 0).length);
        assertNull(Array.box((short[]) null, 0, 0));
        assertEquals(0, Array.box(new short[] { 1 }, 0, 0).length);
        assertNull(Array.box((int[]) null, 0, 0));
        assertEquals(0, Array.box(new int[] { 1 }, 0, 0).length);
        assertNull(Array.box((long[]) null, 0, 0));
        assertEquals(0, Array.box(new long[] { 1L }, 0, 0).length);
        assertNull(Array.box((float[]) null, 0, 0));
        assertEquals(0, Array.box(new float[] { 1F }, 0, 0).length);
        assertNull(Array.box((double[]) null, 0, 0));
        assertEquals(0, Array.box(new double[] { 1D }, 0, 0).length);
        assertNull(Array.box((char[]) null, 0, 0));
        assertEquals(0, Array.box(new char[] { 'a' }, 0, 0).length);
        assertNull(Array.box((boolean[]) null, 0, 0));
        assertEquals(0, Array.box(new boolean[] { true }, 0, 0).length);

        assertNull(Array.unbox((Byte[]) null, 0, 0, (byte) 0));
        assertEquals(0, Array.unbox(new Byte[] { 1 }, 0, 0, (byte) 0).length);
        assertNull(Array.unbox((Short[]) null, 0, 0, (short) 0));
        assertEquals(0, Array.unbox(new Short[] { 1 }, 0, 0, (short) 0).length);
        assertNull(Array.unbox((Integer[]) null, 0, 0, 0));
        assertEquals(0, Array.unbox(new Integer[] { 1 }, 0, 0, 0).length);
        assertNull(Array.unbox((Long[]) null, 0, 0, 0L));
        assertEquals(0, Array.unbox(new Long[] { 1L }, 0, 0, 0L).length);
        assertNull(Array.unbox((Float[]) null, 0, 0, 0F));
        assertEquals(0, Array.unbox(new Float[] { 1F }, 0, 0, 0F).length);
        assertNull(Array.unbox((Double[]) null, 0, 0, 0D));
        assertEquals(0, Array.unbox(new Double[] { 1D }, 0, 0, 0D).length);
        assertNull(Array.unbox((Character[]) null, 0, 0, '\0'));
        assertEquals(0, Array.unbox(new Character[] { 'a' }, 0, 0, '\0').length);
        assertNull(Array.unbox((Boolean[]) null, 0, 0, false));
        assertEquals(0, Array.unbox(new Boolean[] { true }, 0, 0, false).length);

        assertEquals(0, N.flatten((boolean[][]) null).length);
        assertEquals(0, N.flatten((char[][]) null).length);
        assertEquals(0, N.flatten((byte[][]) null).length);
        assertEquals(0, N.flatten((short[][]) null).length);
        assertEquals(0, N.flatten((int[][]) null).length);
        assertEquals(0, N.flatten((long[][]) null).length);
        assertEquals(0, N.flatten((float[][]) null).length);
        assertEquals(0, N.flatten((double[][]) null).length);
        assertArrayEquals(new byte[] { 1, 3 }, N.flatten(new byte[][] { { 1 }, null, { 3 } }));
        assertArrayEquals(new int[] { 1 }, N.concat((int[]) null, new int[] { 1 }));
        assertEquals(0, N.concat((int[]) null, (int[]) null).length);
        assertEquals(0, N.concat(new byte[0], new byte[0]).length);
    }

    @Test
    void typeInterfaceDefaultsStayFalseUntilAClassOverridesThem() {
        final Type<String> stub = stubType();

        assertFalse(stub.isPrimitive());
        assertFalse(stub.isPrimitiveWrapper());
        assertFalse(stub.isPrimitiveList());
        assertFalse(stub.isBoolean());
        assertFalse(stub.isCharacter());
        assertFalse(stub.isNumber());
        assertFalse(stub.isByte());
        assertFalse(stub.isShort());
        assertFalse(stub.isInteger());
        assertFalse(stub.isLong());
        assertFalse(stub.isFloat());
        assertFalse(stub.isDouble());
        assertFalse(stub.isString());
        assertFalse(stub.isCharSequence());
        assertFalse(stub.isDate());
        assertFalse(stub.isCalendar());
        assertFalse(stub.isJodaDateTime());
        assertFalse(stub.isTemporal());
        assertFalse(stub.isPrimitiveArray());
        assertFalse(stub.isPrimitiveByteArray());
        assertFalse(stub.isObjectArray());
        assertFalse(stub.isArray());
        assertFalse(stub.isList());
        assertFalse(stub.isSet());
        assertFalse(stub.isCollection());
        assertFalse(stub.isMap());
        assertFalse(stub.isBean());
        assertFalse(stub.isMapEntity());
        assertFalse(stub.isEntityId());
        assertFalse(stub.isDataset());
        assertFalse(stub.isInputStream());
        assertFalse(stub.isReader());
        assertFalse(stub.isByteBuffer());
        assertFalse(stub.isParameterizedType());
        assertFalse(stub.isImmutable());
        assertTrue(stub.isComparable());
        assertTrue(stub.isSerializable());
        assertFalse(stub.isOptionalOrNullable());
        assertTrue(stub.isCsvQuoteRequired());
        assertFalse(stub.isObject());
    }

    private static void exerciseParallel(final IntStream sequential, final IntStream parallel) {
        final AtomicInteger count = new AtomicInteger();
        sequential.forEachIndexed((index, value) -> count.incrementAndGet());
        parallel.forEachIndexed((index, value) -> count.incrementAndGet());
        assertEquals(6, count.get());
        assertTrue(IntStream.of(1, 2, 3).debounce(Duration.ofMillis(1)).count() >= 1L);
        assertTrue(IntStream.of(1, 2, 3).parallel().debounce(Duration.ofMillis(1)).count() >= 1L);
    }

    private static <T, A, R> R combineTwo(final Collector<T, A, R> collector, final T leftValue, final T rightValue) {
        final A left = collector.supplier().get();
        final A right = collector.supplier().get();
        collector.accumulator().accept(left, leftValue);
        collector.accumulator().accept(right, rightValue);
        return collector.finisher().apply(collector.combiner().apply(left, right));
    }

    private static <T, A, R> R combineWithEmpty(final Collector<T, A, R> collector, final T value) {
        final A empty = collector.supplier().get();
        final A full = collector.supplier().get();
        collector.accumulator().accept(full, value);
        return collector.finisher().apply(collector.combiner().apply(empty, full));
    }

    private static <T, A> A combineEmpty(final Collector<T, A, ?> collector) {
        return collector.combiner().apply(collector.supplier().get(), collector.supplier().get());
    }

    private static <T, A, R> R finishEmpty(final Collector<T, A, R> collector) {
        final A left = collector.supplier().get();
        final A right = collector.supplier().get();
        return collector.finisher().apply(collector.combiner().apply(left, right));
    }

    @SuppressWarnings("unchecked")
    private static Type<String> stubType() {
        return (Type<String>) Proxy.newProxyInstance(Type.class.getClassLoader(), new Class<?>[] { Type.class }, (proxy, method, args) -> {
            if (method.isDefault()) {
                return InvocationHandler.invokeDefault(proxy, method, args);
            }
            if (method.getReturnType() == String.class) {
                return "Stub";
            }
            if (method.getReturnType() == Class.class || method.getReturnType() == java.lang.reflect.Type.class) {
                return String.class;
            }
            if (method.getReturnType() == boolean.class) {
                return false;
            }
            if (method.getReturnType() == int.class) {
                return 0;
            }
            if (List.class.isAssignableFrom(method.getReturnType())) {
                return List.of();
            }
            return null;
        });
    }

    private static FileSystemUtil fileSystem() throws ReflectiveOperationException {
        final Field instance = FileSystemUtil.class.getDeclaredField("INSTANCE");
        instance.setAccessible(true);
        return (FileSystemUtil) instance.get(null);
    }

    private static void deleteTree(final Path root) throws IOException {
        if (Files.notExists(root)) {
            return;
        }
        try (java.util.stream.Stream<Path> walk = Files.walk(root)) {
            final List<Path> paths = walk.sorted(Comparator.reverseOrder()).toList();
            for (final Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    public static class MergeBean {
        @Id
        private Integer id;
        private String name;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    private static final class TextNumber extends Number {
        private static final long serialVersionUID = 1L;

        private final String text;
        private final double value;

        private TextNumber(final String text, final double value) {
            this.text = text;
            this.value = value;
        }

        @Override
        public int intValue() {
            return (int) value;
        }

        @Override
        public long longValue() {
            return (long) value;
        }

        @Override
        public float floatValue() {
            return (float) value;
        }

        @Override
        public double doubleValue() {
            return value;
        }

        @Override
        public String toString() {
            return text;
        }
    }
}
