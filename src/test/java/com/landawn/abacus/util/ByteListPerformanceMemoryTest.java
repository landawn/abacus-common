package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for ByteList. */
@Tag("slow-test")
public class ByteListPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G006 end ----
    // ---- perf review 2026-10-08 G008 begin ----
    @Nested
    class G008 {

        private int referenceHash(final byte[] a, final int size) {
            int h = 1;

            for (int i = 0; i < size; i++) {
                h = 31 * h + a[i];
            }

            return h;
        }

        @SuppressWarnings("deprecation")
        private int capacity(final ByteList list) {
            return list.internalArray().length;
        }

        private void assertHashMatchesContent(final ByteList list) {
            final byte[] content = list.toArray();

            assertEquals(referenceHash(content, content.length), list.hashCode());
            assertEquals(Arrays.hashCode(content), list.hashCode());
        }

        // G008-01: an exactly sized list (Arrays.hashCode path) and a list with spare capacity (range path) hash the same content identically
        @Test
        public void testHashCode_exactAndSpareCapacityAgree() {
            final Random rnd = new Random(20261008L);
            final List<Integer> sizes = new ArrayList<>();

            for (int n = 0; n <= 70; n++) {
                sizes.add(n);
            }

            sizes.addAll(Arrays.asList(127, 128, 255, 256, 257, 1000, 1024, 4096, 65537));

            for (final int n : sizes) {
                for (int round = 0; round < 4; round++) {
                    final byte[] a = new byte[n];

                    if (round == 0 || round == 1) {
                        rnd.nextBytes(a);
                    } else if (round == 2) {
                        Arrays.fill(a, Byte.MIN_VALUE);
                    } else {
                        Arrays.fill(a, (byte) -1);
                    }

                    final int expected = referenceHash(a, n);
                    assertEquals(Arrays.hashCode(a), expected);

                    // exactly sized backing arrays
                    final ByteList exact = ByteList.of(a.clone());
                    assertEquals(n, capacity(exact));
                    assertEquals(expected, exact.hashCode());
                    assertEquals(expected, ByteList.copyOf(a).hashCode());
                    assertEquals(expected, exact.copy().hashCode());
                    assertEquals(expected, ByteIterator.of(a).toList().hashCode());

                    // spare capacity: grown by add, presized, and a window over a larger array whose tail is not zero
                    final ByteList grown = new ByteList();

                    for (final byte b : a) {
                        grown.add(b);
                    }

                    assertEquals(expected, grown.hashCode());

                    final ByteList presized = new ByteList(n + 7);
                    presized.addAll(a);
                    assertTrue(capacity(presized) > presized.size());
                    assertEquals(expected, presized.hashCode());

                    final byte[] wider = Arrays.copyOf(a, n + 5);
                    Arrays.fill(wider, n, n + 5, (byte) 0x5A);
                    final ByteList window = ByteList.of(wider, n);
                    assertTrue(capacity(window) > window.size());
                    assertEquals(expected, window.hashCode());

                    // equal lists hash equal across both paths; trimming moves a list onto the exact path
                    assertEquals(exact, presized);
                    assertEquals(exact.hashCode(), presized.hashCode());
                    assertEquals(exact.hashCode(), window.hashCode());
                    assertEquals(expected, presized.trimToSize().hashCode());
                    assertEquals(n, capacity(presized));
                }
            }
        }

        // G008-01: the hash follows mutations while a list moves between the exact-size and spare-capacity paths
        @Test
        public void testHashCode_tracksMutationsAcrossPaths() {
            final ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
            assertEquals(30817, list.hashCode());
            assertHashMatchesContent(list);

            list.add(Byte.MIN_VALUE);
            assertTrue(capacity(list) > list.size());
            assertHashMatchesContent(list);

            list.trimToSize();
            assertEquals(list.size(), capacity(list));
            assertHashMatchesContent(list);

            list.set(0, Byte.MAX_VALUE);
            assertHashMatchesContent(list);

            list.removeAt(1);
            assertHashMatchesContent(list);

            list.addFirst((byte) -7);
            assertHashMatchesContent(list);

            list.clear();
            assertEquals(1, list.hashCode());
            list.trimToSize();
            assertEquals(0, capacity(list));
            assertEquals(1, list.hashCode());

            assertEquals(1, new ByteList().hashCode());
            assertEquals(1, ByteList.of().hashCode());
            assertEquals(1, new ByteList(16).hashCode());
            assertEquals(31 - 7, ByteList.of((byte) -7).hashCode());
            assertEquals(ByteList.of((byte) 5, (byte) -5).hashCode(), Arrays.asList((byte) 5, (byte) -5).hashCode());
        }
    }
}
