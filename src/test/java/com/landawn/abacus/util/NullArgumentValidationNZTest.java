package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class NullArgumentValidationNZTest extends TestBase {

    @Test
    void factoriesValidateRequiredArgumentsAndPreserveNullableContents() {
        assertThrows(IllegalArgumentException.class, () -> Pair.from((Map.Entry<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Tuple.from((Map.Entry<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Tuple.toList((Tuple.Tuple1<String>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Tuple.toList((Tuple.Tuple9<String, String, String, String, String, String, String, String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Tuple.flatten((Tuple.Tuple2<Tuple.Tuple2<String, String>, String>) null));
        assertThrows(NullPointerException.class, () -> Tuple.flatten(Tuple.of((Tuple.Tuple2<String, String>) null, "tail")));
        assertEquals(Arrays.asList(null, "value"), Tuple.toList(Tuple.of(null, "value")));
        assertThrows(IllegalArgumentException.class, () -> Properties.create(null));
        assertThrows(IllegalArgumentException.class, () -> ReflectASM.on((String) null));
        assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(null));
        assertThrows(IllegalArgumentException.class, () -> Seid.create((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Seid.create(null, Collections.singletonList("id")));
    }

    @Test
    void rangesAndPointsRejectNullBeforeAccess() {
        final Percentage start = Percentage.values()[0];
        assertThrows(IllegalArgumentException.class, () -> Percentage.range(null, start));
        assertThrows(IllegalArgumentException.class, () -> Percentage.range(start, null));
        assertThrows(IllegalArgumentException.class, () -> Percentage.range(start, start, null));
        assertThrows(IllegalArgumentException.class, () -> Percentage.rangeClosed(null, start));
        assertThrows(IllegalArgumentException.class, () -> Percentage.rangeClosed(start, null));
        assertThrows(IllegalArgumentException.class, () -> Percentage.rangeClosed(start, start, null));
        assertThrows(IllegalArgumentException.class, () -> Range.closed(1, 2).span(null));
        final Sheet<String, String, String> sheet = new Sheet<>();
        assertThrows(IllegalArgumentException.class, () -> sheet.get((Sheet.Point) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.set((Sheet.Point) null, "value"));
        assertThrows(IllegalArgumentException.class, () -> sheet.remove((Sheet.Point) null));
        assertThrows(IllegalArgumentException.class, () -> sheet.isNull((Sheet.Point) null));
        assertTrue(sheet.isEmpty());
    }

    @Test
    void requiredConfigurationArgumentsUseIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new ShortSummaryStatistics().combine(null));
        assertThrows(IllegalArgumentException.class, () -> RateLimiter.create(1, 0, null));
        final RateLimiter limiter = RateLimiter.create(1);
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(0, null));
        assertThrows(IllegalArgumentException.class, () -> limiter.tryAcquire(1, 0, null));
        assertTrue(limiter.tryAcquire(0, TimeUnit.SECONDS));
        assertThrows(IllegalArgumentException.class, () -> Stopwatch.createUnstarted().elapsed(null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("ab", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("a", null));
        assertThrows(IllegalArgumentException.class, () -> Utf8.encodedLength(null));
        assertThrows(IllegalArgumentException.class, () -> Utf8.isWellFormed(null));
        assertThrows(IllegalArgumentException.class, () -> Utf8.isWellFormed(null, 0, 0));
        assertTrue(Utf8.isWellFormed(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> ShortList.copyOf(null, 0, 0));
    }

    @Test
    void outputValidationKeepsStreamsUntouched() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.store(null, null, output));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.store(new Properties<>(), null, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.store(new Properties<>(), null, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.storeToXml(null, "root", false, output));
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.storeToXml(new Properties<>(), "root", false, (Writer) null));
        assertEquals(0, output.size());
        assertThrows(IllegalArgumentException.class, () -> PropertiesUtil.formatPath(null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.marshal(null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.unmarshal(String.class, null));
        assertThrows(IllegalArgumentException.class, () -> XmlUtil.unmarshal(null, "<root/>"));
        assertThrows(IllegalArgumentException.class, () -> XmlMappers.wrap(null));
    }

    @Test
    void profilerValidatesReflectiveArgumentsBeforeStartingWorkers() {
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(new Object(), (Method) null, 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(null, "toString", 1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> Profiler.run(new Object(), (String) null, 1, 1, 1));
    }

    @Test
    void nullElementsAndJavaContractsContinueThrowingNullPointerException() {
        final PrefixSearchTable<String, String> table = PrefixSearchTable.<String, String> builder().add(Collections.singletonList("a"), "value").build();
        assertThrows(IllegalArgumentException.class, () -> table.get(null));
        assertThrows(IllegalArgumentException.class, () -> table.getAll(null));
        assertThrows(IllegalArgumentException.class, () -> PrefixSearchTable.builder().add(null, "value"));
        assertThrows(IllegalArgumentException.class, () -> PrefixSearchTable.builder().add(Collections.singletonList("a"), null));
        assertThrows(NullPointerException.class, () -> table.get(Arrays.asList("a", null)));
        assertThrows(NullPointerException.class, () -> u.Optional.of((Object) null));
        assertThrows(NullPointerException.class, () -> u.OptionalInt.empty().compareTo(null));
        assertThrows(NullPointerException.class, () -> ObjIterator.empty().toArray((Object[]) null));
        final Properties<String, String> props = new Properties<>();
        props.set("present", "value");
        assertThrows(IllegalArgumentException.class, () -> props.get("present", (Class<String>) null));
        assertThrows(IllegalArgumentException.class, () -> props.getOrDefault("present", "default", null));
        assertThrows(IllegalArgumentException.class, () -> props.getOrDefault("missing", "default", null));
        assertEquals("default", props.getOrDefault("missing", "default", String.class));
        assertNotNull(table.get(Collections.singletonList("a")));
    }
}
