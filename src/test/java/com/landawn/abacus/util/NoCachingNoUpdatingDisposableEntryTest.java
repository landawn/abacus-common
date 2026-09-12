package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;

public class NoCachingNoUpdatingDisposableEntryTest extends NoCachingNoUpdatingTestSupport {
    @Test
    public void testDisposableEntry_wrap_normal() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertNotNull(disposable);
        assertEquals("key", disposable.getKey());
        assertEquals(123, disposable.getValue());
    }

    @Test
    public void testDisposableEntry_wrap_nullEntry() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableEntry.wrap(null);
        });
    }

    @Test
    public void testDisposableEntry_getKey() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertEquals("key", disposable.getKey());
    }

    @Test
    public void testDisposableEntry_getValue() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertEquals(123, disposable.getValue());
    }

    @Test
    public void testDisposableEntry_setValue_throwsException() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertThrows(UnsupportedOperationException.class, () -> {
            disposable.setValue(456);
        });
    }

    @Test
    public void testDisposableEntry_apply_withEntry() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        String result = disposable.apply(e -> e.getKey() + "=" + e.getValue());
        assertEquals("key=123", result);
    }

    @Test
    public void testDisposableEntry_apply_withBiFunction() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        String result = disposable.apply((k, v) -> k + "=" + v);
        assertEquals("key=123", result);
    }

    @Test
    public void testDisposableEntry_accept_withEntry() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept(e -> ref.set(e.getKey() + "=" + e.getValue()));
        assertEquals("key=123", ref.get());
    }

    @Test
    public void testDisposableEntry_accept_withBiConsumer() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept((k, v) -> ref.set(k + "=" + v));
        assertEquals("key=123", ref.get());
    }

    @Test
    public void testDisposableEntry_copy() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        Map.Entry<String, Integer> copy = disposable.copy();
        assertNotNull(copy);
        assertEquals("key", copy.getKey());
        assertEquals(123, copy.getValue());
        assertNotSame(entry, copy);
    }

    @Test
    public void testDisposableEntry_toString() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        String result = disposable.toString();
        assertNotNull(result);
        assertTrue(result.contains("key"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testDisposableEntryWrap() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals("key", entry.getKey());
        Assertions.assertEquals(Integer.valueOf(100), entry.getValue());
    }

    @Test
    public void testDisposableEntrySetValueUnsupported() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testDisposableEntryCopy() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("test", 42);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Map.Entry<String, Integer> copy = entry.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals("test", copy.getKey());
        Assertions.assertEquals(Integer.valueOf(42), copy.getValue());
    }

    @Test
    public void testDisposableEntryApplyWithFunction() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("age", 25);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        String result = entry.apply(e -> e.getKey() + "=" + e.getValue());
        Assertions.assertEquals("age=25", result);
    }

    @Test
    public void testDisposableEntryApplyWithBiFunction() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("count", 10);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        String result = entry.apply((k, v) -> k + " is " + v);
        Assertions.assertEquals("count is 10", result);
    }

    @Test
    public void testDisposableEntryAcceptWithConsumer() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("value", 100);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        boolean[] called = { false };
        entry.accept(e -> {
            called[0] = true;
            Assertions.assertEquals("value", e.getKey());
            Assertions.assertEquals(Integer.valueOf(100), e.getValue());
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableEntryAcceptWithBiConsumer() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("test", 50);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        boolean[] called = { false };
        entry.accept((k, v) -> {
            called[0] = true;
            Assertions.assertEquals("test", k);
            Assertions.assertEquals(Integer.valueOf(50), v);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableEntryToString() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 123);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Assertions.assertEquals("key=123", entry.toString());
    }

    @Test
    public void testDisposableEntry_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableEntry.wrap(null));
    }

    @Test
    public void testDisposableEntry_withNullKeyValue() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>(null, 100);
        DisposableEntry<String, Integer> disposable1 = DisposableEntry.wrap(entry1);
        assertNull(disposable1.getKey());
        assertEquals(Integer.valueOf(100), disposable1.getValue());

        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", null);
        DisposableEntry<String, Integer> disposable2 = DisposableEntry.wrap(entry2);
        assertEquals("key", disposable2.getKey());
        assertNull(disposable2.getValue());

        Map.Entry<String, Integer> entry3 = new AbstractMap.SimpleEntry<>(null, null);
        DisposableEntry<String, Integer> disposable3 = DisposableEntry.wrap(entry3);
        assertNull(disposable3.getKey());
        assertNull(disposable3.getValue());
    }

    @Test
    public void testDisposableEntry_hashCodeAndEquals() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 100);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", 100);

        DisposableEntry<String, Integer> disposable1 = DisposableEntry.wrap(entry1);
        DisposableEntry<String, Integer> disposable2 = DisposableEntry.wrap(entry2);

        assertEquals(disposable1.hashCode(), disposable2.hashCode());
        assertEquals(disposable1, disposable2);
        assertEquals(entry1, disposable1);
        assertEquals(disposable1, entry1);
    }

    @Test
    public void testDisposableEntry_usesMapEntryArrayEqualitySemantics() {
        final Object[] key1 = { "key" };
        final Object[] key2 = { "key" };
        final Map.Entry<Object[], Integer> entry1 = new AbstractMap.SimpleEntry<>(key1, 100);
        final Map.Entry<Object[], Integer> entry2 = new AbstractMap.SimpleEntry<>(key2, 100);
        final DisposableEntry<Object[], Integer> disposable = DisposableEntry.wrap(entry1);

        assertNotEquals(disposable, entry2);
        assertNotEquals(entry2, disposable);
    }

    @Test
    public void testDisposableEntry_wrap() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);

        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
    }

    @Test
    public void testDisposableEntry_setValueThrows() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testDisposableEntry_apply() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);

        String result1 = entry.apply(e -> e.getKey() + ":" + e.getValue());
        assertEquals("key:100", result1);

        String result2 = entry.apply((k, v) -> k + "=" + v);
        assertEquals("key=100", result2);
    }

    @Test
    public void testDisposableEntry_accept() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);

        final boolean[] called1 = { false };
        entry.accept(e -> called1[0] = true);
        assertTrue(called1[0]);

        final String[] result = { "" };
        entry.accept((k, v) -> result[0] = k + ":" + v);
        assertEquals("key:100", result[0]);
    }

    @Test
    public void testDisposableEntry_setValueIsUnsupportedWhileTheContentsStayProducerControlled() {
        final AbstractMap.SimpleEntry<String, Integer> live = new AbstractMap.SimpleEntry<>("k", 1);
        final DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(live);

        assertThrows(UnsupportedOperationException.class, () -> disposable.setValue(7));

        live.setValue(99);
        assertEquals(99, disposable.getValue());

        final Map.Entry<String, Integer> copy = disposable.copy();
        copy.setValue(1234);
        assertEquals(1234, copy.getValue());
        assertEquals(99, disposable.getValue());

        live.setValue(5);
        assertEquals(1234, copy.getValue());
        assertEquals(5, disposable.getValue());
    }

    @Test
    public void testDisposableEntry_hashCodeAndEqualsTrackTheCurrentKeyAndValue() {
        final AbstractMap.SimpleEntry<String, Integer> live = new AbstractMap.SimpleEntry<>("k", 1);
        final DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(live);

        final Set<Map.Entry<String, Integer>> seen = new HashSet<>();
        seen.add(disposable);
        assertTrue(seen.contains(new AbstractMap.SimpleEntry<>("k", 1)));

        final int hashBefore = disposable.hashCode();
        live.setValue(99);
        assertTrue(hashBefore != disposable.hashCode());
        assertFalse(seen.contains(disposable));
        assertEquals(1, seen.size());
        assertSame(disposable, seen.iterator().next());

        assertFalse(disposable.equals(new AbstractMap.SimpleEntry<>("k", 1)));
        assertTrue(disposable.equals(new AbstractMap.SimpleEntry<>("k", 99)));

        final Set<Map.Entry<String, Integer>> snapshots = new HashSet<>();
        snapshots.add(disposable.copy());
        live.setValue(7);
        assertTrue(snapshots.contains(new AbstractMap.SimpleEntry<>("k", 99)));
        assertFalse(snapshots.contains(disposable));
    }
}
