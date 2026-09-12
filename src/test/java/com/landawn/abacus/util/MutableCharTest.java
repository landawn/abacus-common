package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class MutableCharTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals('A', MutableChar.of('A').value());
        assertEquals('\n', MutableChar.of('\n').value());
        assertEquals('\t', MutableChar.of('\t').value());
        assertEquals(' ', MutableChar.of(' ').value());
        assertEquals('\0', MutableChar.of('\0').value());
        assertEquals('\uffff', MutableChar.of('\uffff').value());
        assertEquals('\u4E2D', MutableChar.of('\u4E2D').value());
        assertEquals('A', MutableChar.of('\u0041').value());
        assertEquals('X', MutableChar.of('X').getValue());
    }

    @Test
    public void testGetAndSet() {
        MutableChar mc = MutableChar.of('A');
        mc.setValue('Z');
        assertEquals('Z', mc.value());
        mc.setValue('\0');
        assertEquals('\0', mc.value());
        mc.setValue('\uffff');
        assertEquals('\uffff', mc.value());
        mc.setValue('\\');
        assertEquals('\\', mc.value());

        assertEquals('\\', mc.getAndSet('B'));
        assertEquals('B', mc.value());
        assertEquals('B', mc.getAndSet('B'));
        assertEquals('C', mc.setAndGet('C'));
        assertEquals('C', mc.value());
        assertEquals('\0', mc.setAndGet('\0'));
    }

    @Test
    public void testSetIf() throws Exception {
        MutableChar mc = MutableChar.of('A');
        assertTrue(mc.setIf(c -> c < 'M', 'Z'));
        assertEquals('Z', mc.value());
        assertFalse(mc.setIf(c -> c < 'M', 'A'));
        assertEquals('Z', mc.value());
        assertTrue(MutableChar.of('B').setIf(c -> true, 'C'));
        assertFalse(MutableChar.of('B').setIf(c -> false, 'C'));
        assertTrue(MutableChar.of('\0').setIf(c -> c == '\0', 'A'));
    }

    @Test
    public void testIncrementDecrement() {
        MutableChar mc = MutableChar.of('A');
        mc.increment();
        assertEquals('B', mc.value());
        mc.decrement();
        assertEquals('A', mc.value());

        MutableChar unicode = MutableChar.of('\u4E2D');
        unicode.increment();
        assertEquals('\u4E2E', unicode.value());

        MutableChar overflow = MutableChar.of('\uffff');
        overflow.increment();
        assertEquals('\0', overflow.value());
        overflow.decrement();
        assertEquals('\uffff', overflow.value());

        MutableChar gi = MutableChar.of('A');
        assertEquals('A', gi.getAndIncrement());
        assertEquals('B', gi.value());
        MutableChar giOverflow = MutableChar.of('\uffff');
        assertEquals('\uffff', giOverflow.getAndIncrement());
        assertEquals('\0', giOverflow.value());

        MutableChar gd = MutableChar.of('B');
        assertEquals('B', gd.getAndDecrement());
        assertEquals('A', gd.value());
        MutableChar gdUnderflow = MutableChar.of('\0');
        assertEquals('\0', gdUnderflow.getAndDecrement());
        assertEquals('\uffff', gdUnderflow.value());

        MutableChar ig = MutableChar.of('A');
        assertEquals('B', ig.incrementAndGet());
        assertEquals('B', ig.value());
        assertEquals('\0', MutableChar.of('\uffff').incrementAndGet());

        MutableChar dg = MutableChar.of('B');
        assertEquals('A', dg.decrementAndGet());
        assertEquals('A', dg.value());
        assertEquals('\uffff', MutableChar.of('\0').decrementAndGet());
    }

    @Test
    public void testAddSubtract() {
        MutableChar mc = MutableChar.of('A');
        mc.add((char) 2);
        assertEquals('C', mc.value());
        mc.add((char) 0);
        assertEquals('C', mc.value());
        mc.subtract((char) 2);
        assertEquals('A', mc.value());
        mc.subtract((char) 0);
        assertEquals('A', mc.value());

        MutableChar overflow = MutableChar.of(Character.MAX_VALUE);
        overflow.add((char) 1);
        assertEquals('\0', overflow.value());
        overflow.subtract((char) 1);
        assertEquals(Character.MAX_VALUE, overflow.value());

        MutableChar ga = MutableChar.of('A');
        assertEquals('A', ga.getAndAdd((char) 2));
        assertEquals('C', ga.value());
        MutableChar gaOverflow = MutableChar.of(Character.MAX_VALUE);
        assertEquals(Character.MAX_VALUE, gaOverflow.getAndAdd((char) 1));
        assertEquals('\0', gaOverflow.value());

        MutableChar ag = MutableChar.of('A');
        assertEquals('C', ag.addAndGet((char) 2));
        assertEquals('C', ag.value());
        assertEquals('\0', MutableChar.of(Character.MAX_VALUE).addAndGet((char) 1));
    }

    @Test
    public void testCompareTo() {
        MutableChar a = MutableChar.of('A');
        MutableChar b = MutableChar.of('B');
        MutableChar c = MutableChar.of('A');
        assertEquals(0, a.compareTo(a));
        assertEquals(0, a.compareTo(c));
        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertEquals(0, MutableChar.of('\0').compareTo(MutableChar.of('\0')));
        assertTrue(MutableChar.of('\ufffe').compareTo(MutableChar.of('\uffff')) < 0);
        assertThrows(NullPointerException.class, () -> a.compareTo(null));
    }

    @Test
    public void testEqualsHashCodeToString() {
        MutableChar a = MutableChar.of('A');
        MutableChar b = MutableChar.of('A');
        MutableChar c = MutableChar.of('B');
        assertEquals(a, a);
        assertTrue(a.equals(b));
        assertFalse(a.equals(c));
        assertFalse(a.equals(null));
        assertFalse(a.equals("A"));
        assertFalse(a.equals(Character.valueOf('A')));
        assertFalse(a.equals(65));
        assertTrue(MutableChar.of('\0').equals(MutableChar.of('\0')));
        assertTrue(MutableChar.of('\uffff').equals(MutableChar.of('\uffff')));

        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(65, a.hashCode());
        assertEquals(0, MutableChar.of('\0').hashCode());
        assertEquals(65535, MutableChar.of('\uffff').hashCode());

        assertEquals("A", MutableChar.of('A').toString());
        assertEquals("\n", MutableChar.of('\n').toString());
        assertEquals("\t", MutableChar.of('\t').toString());
        assertEquals("5", MutableChar.of('5').toString());
        assertEquals("@", MutableChar.of('@').toString());
        assertEquals("\0", MutableChar.of('\0').toString());
    }

    /**
     * G54-001 / G54-002: {@code hashCode()} is the char value, so equal values hash alike - but the
     * value is mutable, and mutating a live key makes the map entry unreachable in its old bucket.
     */
    @Test
    public void testHashCodeAndHashKeyHazard() {
        assertEquals(65, MutableChar.of('A').hashCode());
        assertEquals(MutableChar.of('A').hashCode(), MutableChar.of('A').hashCode());

        final MutableChar key = MutableChar.of('A');
        final Map<MutableChar, String> map = new HashMap<>();
        map.put(key, "v");
        assertEquals("v", map.get(MutableChar.of('A')));

        key.increment(); // 'A' -> 'B'

        assertFalse(map.containsKey(key));
        assertFalse(map.containsKey(MutableChar.of('A')));
        assertFalse(map.containsKey(MutableChar.of('B')));
        assertEquals(1, map.size()); // still there, just unreachable
    }
}
