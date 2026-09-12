package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;

public class SeqAppendTest extends SeqTestSupport {

    @Test
    public void testAppend() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2).append(3).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), Seq.of(1, 2).append(3, 4, 5).toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).append(new Integer[0]).toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> empty().append(1, 2, 3).toList());

        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppend_Collection() throws Exception {
        assertEquals(Arrays.asList("a", "b", "c", "d"), Seq.of("a", "b").append(Arrays.asList("c", "d")).toList());
        assertEquals(Arrays.asList("x", "y"), Seq.of("x", "y").append(Collections.emptyList()).toList());
        assertArrayEquals(new String[] { "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppend_Seq() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3, 4), Seq.of(1, 2).append(Seq.of(3, 4)).toList());
        assertEquals(Arrays.asList(1, 2), Seq.of(1, 2).append(Seq.<Integer, Exception> empty()).toList());
        assertEquals(Arrays.asList(1, 2, 3, 4), Seq.of(1, 2).append(Seq.of(Arrays.asList(3, 4))).toList());
    }

    @Test
    public void testAppend_Optional() throws Exception {
        assertEquals(Arrays.asList("hello", "world"), Seq.of("hello").append(Optional.of("world")).toList());
        assertEquals(Arrays.asList("hello"), Seq.of("hello").append(Optional.<String> empty()).toList());
        assertEquals(Arrays.asList("world"), Seq.<String, Exception> empty().append(Optional.of("world")).toList());
        assertArrayEquals(new String[] { "c", "d", "e", "x" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendIfEmpty() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> empty().appendIfEmpty(1, 2, 3).toList());
        assertEquals(Arrays.asList(4, 5), Seq.of(4, 5).appendIfEmpty(1, 2, 3).toList());
        assertEquals(Arrays.asList("default1", "default2"), Seq.<String, Exception> empty().appendIfEmpty(Arrays.asList("default1", "default2")).toList());
        assertEquals(Arrays.asList("value"), Seq.of("value").appendIfEmpty(Arrays.asList("default1", "default2")).toList());
        assertTrue(Seq.<String, Exception> empty().appendIfEmpty(Collections.emptyList()).toList().isEmpty());
        assertEquals(CommonUtil.emptyList(), Seq.<Integer, Exception> empty().appendIfEmpty((Integer[]) null).toList());
        assertEquals(CommonUtil.asList(1), Seq.<Integer, Exception> of(1).appendIfEmpty((Integer[]) null).toList());
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendIfEmpty_Supplier() throws Exception {
        Supplier<Seq<Integer, Exception>> defaults = () -> Seq.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> empty().appendIfEmpty(defaults).toList());
        assertEquals(Arrays.asList(4, 5), Seq.of(4, 5).appendIfEmpty(defaults).toList());
        assertTrue(Seq.<Integer, Exception> empty().appendIfEmpty(() -> Seq.<Integer, Exception> empty()).toList().isEmpty());
    }

    @Test
    public void appendIfEmpty_nonEmptyPathDoesNotReturnReceiver() throws Exception {
        final Seq<Integer, Exception> seq = Seq.of(4, 5);
        final Seq<Integer, Exception> derived = seq.appendIfEmpty(1, 2, 3);
        assertTrue(seq != derived);
        assertEquals(Arrays.asList(4, 5), derived.toList());
        assertThrows(IllegalStateException.class, seq::toList);
    }

    // --- G12-003: append(Seq)/prepend(Seq) used to reject an already-closed receiver before the argument was ever
    // --- handed to concat(..), stranding it. zipWith/mergeWith close every input on the same failure.
    @Test
    public void testAppend_Seq_closedReceiverStillClosesTheArgument() throws Exception {
        final AtomicInteger argClosed = new AtomicInteger();
        final Seq<Integer, Exception> arg = Seq.<Integer, Exception> of(8, 9).onClose(argClosed::incrementAndGet);

        final Seq<Integer, Exception> receiver = Seq.of(1, 2, 3);
        receiver.count();

        assertThrows(IllegalStateException.class, () -> receiver.append(arg));
        assertEquals(1, argClosed.get());
    }

    @Test
    public void testPrepend_Seq_closedReceiverStillClosesTheArgument() throws Exception {
        final AtomicInteger argClosed = new AtomicInteger();
        final Seq<Integer, Exception> arg = Seq.<Integer, Exception> of(8, 9).onClose(argClosed::incrementAndGet);

        final Seq<Integer, Exception> receiver = Seq.of(1, 2, 3);
        receiver.count();

        assertThrows(IllegalStateException.class, () -> receiver.prepend(arg));
        assertEquals(1, argClosed.get());
    }

    @Test
    public void testAppendPrepend_Seq_successPathStillTakesOwnershipOfTheArgument() throws Exception {
        final AtomicInteger appendArgClosed = new AtomicInteger();
        assertEquals(Arrays.asList(1, 9), Seq.<Integer, Exception> of(1).append(Seq.<Integer, Exception> of(9).onClose(appendArgClosed::incrementAndGet)).toList());
        assertEquals(1, appendArgClosed.get());

        final AtomicInteger prependArgClosed = new AtomicInteger();
        assertEquals(Arrays.asList(9, 1), Seq.<Integer, Exception> of(1).prepend(Seq.<Integer, Exception> of(9).onClose(prependArgClosed::incrementAndGet)).toList());
        assertEquals(1, prependArgClosed.get());
    }

    // --- G12-007 (doc): neither overload can raise IllegalArgumentException - a null sequence is tolerated - so the
    // --- declared-but-never-thrown IAE was dropped from both signatures.
    @Test
    public void testAppendPrepend_Seq_nullArgumentIsTolerated() throws Exception {
        assertEquals(Arrays.asList(1), Seq.<Integer, Exception> of(1).append((Seq<Integer, Exception>) null).toList());
        assertEquals(Arrays.asList(1), Seq.<Integer, Exception> of(1).prepend((Seq<Integer, Exception>) null).toList());
    }
}
