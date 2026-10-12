package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class EntryStreamPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G095 end ----
    // ---- perf review 2026-10-08 G096 begin ----
    @Nested
    class G096 {

        private Map<String, Integer> source() {
            final Map<String, Integer> map = new LinkedHashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);
            return map;
        }

        private Map<String, Integer> bigSource() {
            final Map<String, Integer> map = new LinkedHashMap<>();

            for (int i = 0; i < 60; i++) {
                map.put("k" + i, i + 1);
            }

            return map;
        }

        private Map.Entry<String, Integer> entry(final String key, final Integer value) {
            return new java.util.AbstractMap.SimpleImmutableEntry<>(key, value);
        }

        private <T> Stream<T> track(final Stream<T> stream, final AtomicInteger closeCount, final List<Stream<?>> returned) {
            final Stream<T> result = stream.onClose(closeCount::incrementAndGet);
            returned.add(result);
            return result;
        }

        private List<String> sortedStrings(final List<Map.Entry<String, Integer>> entries) {
            final List<String> result = new ArrayList<>();

            for (final Map.Entry<String, Integer> e : entries) {
                result.add(e.getKey() + "=" + e.getValue());
            }

            Collections.sort(result);
            return result;
        }

        // G096-01: flatMapKey (Function and BiFunction forms): order of mapper calls, inner elements, inner close and outer close
        @Test
        public void testFlatMapKey_sequentialCallAndCloseOrder() {
            final List<String> expected = Arrays.asList("map:a", "elem:a1", "out:a1=1", "elem:a2", "out:a2=1", "close:a", "map:b", "elem:b1", "out:b1=2",
                    "elem:b2", "out:b2=2", "close:b", "map:c", "elem:c1", "out:c1=3", "elem:c2", "out:c2=3", "close:c", "outer-close");

            final List<String> log = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapKey(k -> {
                log.add("map:" + k);
                return Stream.of(k + "1", k + "2").peek(x -> log.add("elem:" + x)).onClose(() -> log.add("close:" + k));
            }).forEach((k, v) -> log.add("out:" + k + "=" + v));
            assertEquals(expected, log);

            final List<String> log2 = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log2.add("outer-close")).flatMapKey((k, v) -> {
                log2.add("map:" + k);
                return Stream.of(k + "1", k + "2").peek(x -> log2.add("elem:" + x)).onClose(() -> log2.add("close:" + k));
            }).forEach((k, v) -> log2.add("out:" + k + "=" + v));
            assertEquals(expected, log2);
        }

        // G096-01: flatMapValue (Function and BiFunction forms): order of mapper calls, inner elements, inner close and outer close
        @Test
        public void testFlatMapValue_sequentialCallAndCloseOrder() {
            final List<String> expected = Arrays.asList("map:1", "elem:10", "out:a=10", "elem:11", "out:a=11", "close:1", "map:2", "elem:20", "out:b=20",
                    "elem:21", "out:b=21", "close:2", "map:3", "elem:30", "out:c=30", "elem:31", "out:c=31", "close:3", "outer-close");

            final List<String> log = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapValue(v -> {
                log.add("map:" + v);
                return Stream.of(v * 10, v * 10 + 1).peek(x -> log.add("elem:" + x)).onClose(() -> log.add("close:" + v));
            }).forEach((k, v) -> log.add("out:" + k + "=" + v));
            assertEquals(expected, log);

            final List<String> log2 = new ArrayList<>();
            EntryStream.of(source()).onClose(() -> log2.add("outer-close")).flatMapValue((k, v) -> {
                log2.add("map:" + v);
                return Stream.of(v * 10, v * 10 + 1).peek(x -> log2.add("elem:" + x)).onClose(() -> log2.add("close:" + v));
            }).forEach((k, v) -> log2.add("out:" + k + "=" + v));
            assertEquals(expected, log2);
        }

        // G096-01: every stream returned by the mapper is closed exactly once and cannot be reused (all 4 forms, sequential and parallel)
        @Test
        public void testFlatMapKeyValue_returnedStreamsClosedOnce() {
            for (final boolean parallel : new boolean[] { false, true }) {
                for (int form = 0; form < 4; form++) {
                    final Map<String, Integer> src = bigSource();
                    final AtomicInteger closeCount = new AtomicInteger();
                    final List<Stream<?>> returned = Collections.synchronizedList(new ArrayList<>());
                    final EntryStream<String, Integer> s = parallel ? EntryStream.of(src).parallel(3) : EntryStream.of(src);
                    final List<Map.Entry<String, Integer>> result;
                    final List<String> expected = new ArrayList<>();

                    switch (form) {
                        case 0:
                            result = s.flatMapKey(k -> track(Stream.of(k, k + "'"), closeCount, returned)).toList();
                            break;
                        case 1:
                            result = s.flatMapKey((k, v) -> track(Stream.of(k, k + "'"), closeCount, returned)).toList();
                            break;
                        case 2:
                            result = s.flatMapValue(v -> track(Stream.of(v, -v), closeCount, returned)).toList();
                            break;
                        default:
                            result = s.flatMapValue((k, v) -> track(Stream.of(v, -v), closeCount, returned)).toList();
                            break;
                    }

                    for (final Map.Entry<String, Integer> e : src.entrySet()) {
                        if (form < 2) {
                            expected.add(e.getKey() + "=" + e.getValue());
                            expected.add(e.getKey() + "'=" + e.getValue());
                        } else {
                            expected.add(e.getKey() + "=" + e.getValue());
                            expected.add(e.getKey() + "=" + (-e.getValue()));
                        }
                    }

                    Collections.sort(expected);
                    final String label = "parallel=" + parallel + ", form=" + form;
                    assertEquals(expected, sortedStrings(result), label);
                    assertEquals(src.size(), closeCount.get(), label);
                    assertEquals(src.size(), returned.size(), label);

                    for (final Stream<?> r : returned) {
                        assertThrows(IllegalStateException.class, r::count, label);
                    }

                    assertEquals(src.size(), closeCount.get(), label);
                }
            }
        }

        // G096-01: a short-circuit closes only the inner stream that was opened, exactly once
        @Test
        public void testFlatMapKeyValue_shortCircuitClosesOpenedInnerStreamOnce() {
            final List<String> log = new ArrayList<>();
            assertEquals(entry("a1", 1), EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapKey(k -> {
                log.add("map:" + k);
                return Stream.of(k + "1", k + "2").onClose(() -> log.add("close:" + k));
            }).first().get());
            assertEquals(Arrays.asList("map:a", "outer-close", "close:a"), log);

            log.clear();
            assertEquals(Arrays.asList(entry("a", 10)), EntryStream.of(source()).onClose(() -> log.add("outer-close")).flatMapValue((k, v) -> {
                log.add("map:" + k);
                return Stream.of(v * 10, v * 10 + 1).onClose(() -> log.add("close:" + k));
            }).limit(1).toList());
            assertEquals(Arrays.asList("map:a", "outer-close", "close:a"), log);

            // closing the returned EntryStream while an inner stream is being consumed closes that inner stream once
            log.clear();
            final List<Stream<?>> returned = new ArrayList<>();
            final EntryStream<String, Integer> s = EntryStream.of(source()).flatMapKey(k -> {
                log.add("map:" + k);
                final Stream<String> inner = Stream.of(k + "1", k + "2").onClose(() -> log.add("close:" + k));
                returned.add(inner);
                return inner;
            });
            final Iterator<Map.Entry<String, Integer>> iter = s.iterator();
            assertEquals(entry("a1", 1), iter.next());
            s.close();
            s.close();
            assertEquals(Arrays.asList("map:a", "close:a"), log);
            assertEquals(1, returned.size());
            assertThrows(IllegalStateException.class, () -> returned.get(0).count());
        }

        // G096-01: a failing close handler of a returned stream propagates unchanged; it runs once and the outer stream is closed once
        @Test
        public void testFlatMapKeyValue_innerCloseFailurePropagates() {
            final AtomicInteger outerClosed = new AtomicInteger();
            final AtomicInteger innerClosed = new AtomicInteger();
            final IllegalStateException failure = new IllegalStateException("inner-close-failure");
            final List<Stream<?>> returned = new ArrayList<>();

            final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                    () -> EntryStream.of(source()).onClose(outerClosed::incrementAndGet).flatMapKey(k -> {
                        final Stream<String> inner = Stream.of(k).onClose(() -> {
                            innerClosed.incrementAndGet();
                            throw failure;
                        });
                        returned.add(inner);
                        return inner;
                    }).toList());

            assertSame(failure, thrown);
            assertEquals(1, innerClosed.get());
            assertEquals(1, outerClosed.get());
            assertEquals(1, returned.size());
            assertThrows(IllegalStateException.class, () -> returned.get(0).count());
            assertEquals(1, innerClosed.get());
        }

        // G096-01: an already-closed returned stream, or the same stream returned twice, fails as before and closes the outer stream
        @Test
        public void testFlatMapKeyValue_closedOrReusedReturnedStreamFails() {
            final Stream<String> closedStream = Stream.of("x");
            closedStream.close();
            final AtomicInteger outerClosed = new AtomicInteger();
            final IllegalStateException e1 = assertThrows(IllegalStateException.class,
                    () -> EntryStream.of(source()).onClose(outerClosed::incrementAndGet).flatMapKey(k -> closedStream).toList());
            assertEquals("This stream is already terminated.", e1.getMessage());
            assertEquals(1, outerClosed.get());

            final Stream<Integer> shared = Stream.of(7, 8);
            final List<Map.Entry<String, Integer>> seen = new ArrayList<>();
            final AtomicInteger outerClosed2 = new AtomicInteger();
            final IllegalStateException e2 = assertThrows(IllegalStateException.class,
                    () -> EntryStream.of(source()).onClose(outerClosed2::incrementAndGet).flatMapValue((k, v) -> shared).forEach(e -> seen.add(e)));
            assertEquals("This stream is already terminated.", e2.getMessage());
            assertEquals(Arrays.asList(entry("a", 7), entry("a", 8)), seen);
            assertEquals(1, outerClosed2.get());
        }

        // G096-01: handlers of a returned derived stream (and of its parent) run once each, parent first
        @Test
        public void testFlatMapValue_derivedReturnedStreamHandlerOrder() {
            final List<String> log = new ArrayList<>();
            final Stream<Integer> parent = Stream.of(1, 2).onClose(() -> log.add("h1"));
            final Stream<Integer> derived = parent.map(x -> x * 10).onClose(() -> log.add("h2"));

            assertEquals(Arrays.asList(entry("a", 10), entry("a", 20)), EntryStream.of("a", 1).flatMapValue(v -> derived).toList());
            assertEquals(Arrays.asList("h1", "h2"), log);
            assertThrows(IllegalStateException.class, () -> parent.count());
            assertThrows(IllegalStateException.class, () -> derived.count());
            assertEquals(Arrays.asList("h1", "h2"), log);

            // a null result is still an empty expansion, for both the key and the value forms
            assertEquals(Arrays.asList(entry("b", 2)), EntryStream.of(source()).flatMapKey(k -> "b".equals(k) ? Stream.of(k) : (Stream<String>) null).toList());
            assertEquals(Arrays.asList(entry("c", 3)),
                    EntryStream.of(source()).flatMapValue((k, v) -> "c".equals(k) ? Stream.of(v) : (Stream<Integer>) null).toList());
        }
    }
}
