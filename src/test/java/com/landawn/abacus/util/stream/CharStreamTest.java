package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.u.OptionalChar;

/** CharStream general contracts. */
public class CharStreamTest extends CharStreamTestSupport {

    // append

    @Test
    public void testStreamCreatedAfterAppendArray() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendOptional() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAsIntStream() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().boxed().toList());
        assertEquals(N.toList(2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).boxed().toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().boxed().toList());
        assertEquals(N.toList(2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).count());
        assertArrayEquals(new Character[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().toArray(Character[]::new));
        assertArrayEquals(new Character[] { (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).toArray(Character[]::new));
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Character[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().toArray(Character[]::new));
        assertArrayEquals(new Character[] { (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).toArray(Character[]::new));
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).toList());
    }

    // collapse

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).skip(1).count());
        List<CharList> collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, collapsed.size());
        assertEquals(CharList.of((char) 1, (char) 2), collapsed.get(0));
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(1));
        assertEquals(CharList.of((char) 7), collapsed.get(2));
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).skip(1).toList();
        assertEquals(2, collapsed.size());
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(0));
        assertEquals(CharList.of((char) 7), collapsed.get(1));
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).count());
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, collapsed.size());
        assertEquals(CharList.of((char) 1, (char) 2), collapsed.get(0));
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(1));
        assertEquals(CharList.of((char) 7), collapsed.get(2));
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).toList();
        assertEquals(2, collapsed.size());
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(0));
        assertEquals(CharList.of((char) 7), collapsed.get(1));
    }

    @Test
    public void testStreamCreatedAfterCollapseTriWithMerge() {
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedByConcatArrays() {
        assertEquals(5, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).count());
        assertEquals(4, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).toList());
        assertEquals(5, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).count());
        assertEquals(4, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' },
                CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' },
                CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(10, CharStream.of((char) 1, (char) 2).cycled().limit(10).count());
        assertEquals(8, CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled().limit(10).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).cycled().limit(10).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).toList());
        assertEquals(10, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).count());
        assertEquals(8, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(6, CharStream.of((char) 1, (char) 2).cycled(3).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2).cycled(3).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled(3).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled(3).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).cycled(3).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).cycled(3).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).toList());
    }

    // defaultIfEmpty

    @Test
    public void testDefaultIfEmpty_OnEmpty() {
        CharStream stream = CharStream.empty();
        char[] result = stream.defaultIfEmpty(() -> CharStream.of('x')).toArray();
        assertArrayEquals(new char[] { 'x' }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnNonEmpty() {
        CharStream stream = createCharStream('a', 'b');
        char[] result = stream.defaultIfEmpty(() -> CharStream.of('x')).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        char[] result = CharStream.empty().defaultIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'z' }, result);
    }

    @Test
    public void testDefaultIfEmptyOnNonEmpty() {
        char[] result = createCharStream('a', 'b').defaultIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testStreamCreatedByDefer() {
        assertEquals(5, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).count());
        assertEquals(4, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).toList());
        assertEquals(5, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).count());
        assertEquals(4, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        List<Character> collection = N.toList((char) 2, (char) 3, (char) 6);
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        List<Character> dropped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    // empty

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());

        assertEquals(2, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(1, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 5), CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyArray() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).toList());

        assertEquals(2, CharStream.empty().appendIfEmpty((char) 4, (char) 5).count());
        assertEquals(1, CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().appendIfEmpty((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.empty().appendIfEmpty((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 5), CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());

        assertEquals(2, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(1, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 5), CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(0, CharStream.empty().ifEmpty(() -> actionExecuted.set(true)).count());
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).toList());

        try {
            CharStream.empty().throwIfEmpty().count();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).toList());
        assertEquals(5,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).count());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .toList());

        try {
            CharStream.empty().throwIfEmpty(() -> new IllegalStateException("Empty")).count();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Empty", e.getMessage());
        }
    }

    @Test
    public void testStreamCreatedByEmpty() {
        assertEquals(0, CharStream.empty().count());
        assertEquals(0, CharStream.empty().skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().skip(1).toArray());
        assertEquals(N.toList(), CharStream.empty().toList());
        assertEquals(N.toList(), CharStream.empty().skip(1).toList());
        assertEquals(0, CharStream.empty().map(e -> e).count());
        assertEquals(0, CharStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().map(e -> e).toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(N.toList(), CharStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), CharStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        List<Character> dropped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testFiniteFactoryIteratorsRemainExhaustedAfterFailedNext() {
        final CharIteratorEx[] iterators = { CharStream.range('a', 'c').iteratorEx(), CharStream.range('a', 'e', 2).iteratorEx(),
                CharStream.rangeClosed('a', 'c').iteratorEx(), CharStream.rangeClosed('a', 'e', 2).iteratorEx(), CharStream.repeat('x', 10).iteratorEx() };

        for (final CharIteratorEx iter : iterators) {
            iter.advance(Long.MAX_VALUE);
            assertThrows(NoSuchElementException.class, iter::nextChar);
            assertEquals(0, iter.count());
            assertArrayEquals(new char[0], iter.toArray());
        }
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapArray() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMapArray(e -> new char[] { e, (char) (e + 10) }).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMapArray(e -> new char[] { e, (char) (e + 10) }).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMapArray(e -> new char[] { e, (char) (e + 10) }).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMapArray(e -> new char[] { e, (char) (e + 10) }).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMapArray(e -> new char[] { e, (char) (e + 10) }).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMapArray(e -> new char[] { e, (char) (e + 10) }).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapArray(e -> new char[] { e, (char) (e + 10) }).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapArray(e -> new char[] { e, (char) (e + 10) }).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapArray(e -> new char[] { e, (char) (e + 10) }).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapArray(e -> new char[] { e, (char) (e + 10) }).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapArray(e -> new char[] { e, (char) (e + 10) }).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapArray(e -> new char[] { e, (char) (e + 10) }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 2, 20, 3, 30 }, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30), CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).boxed().toList());
        assertEquals(N.toList(2, 20, 3, 30), CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).boxed().toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 2, 20, 3, 30 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).boxed().toList());
        assertEquals(N.toList(2, 20, 3, 30),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(6, CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObjCollection() {
        assertEquals(6, CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toList());
    }

    // flatten

    @Test
    public void testStreamCreatedByFlatten2D() {
        char[][] array = new char[][] { { 'a', 'b' }, { 'c', 'd', 'e' } };
        assertEquals(5, CharStream.flatten(array).count());
        assertEquals(4, CharStream.flatten(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).skip(1).toList());
        assertEquals(5, CharStream.flatten(array).map(e -> e).count());
        assertEquals(4, CharStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlatten3D() {
        char[][][] array = new char[][][] { { { 'a', 'b' }, { 'c' } }, { { 'd', 'e' } } };
        assertEquals(5, CharStream.flatten(array).count());
        assertEquals(4, CharStream.flatten(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).skip(1).toList());
        assertEquals(5, CharStream.flatten(array).map(e -> e).count());
        assertEquals(4, CharStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlattenVertically() {
        char[][] array = new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        assertEquals(6, CharStream.flatten(array, true).count());
        assertEquals(5, CharStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).toArray());
        assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.toList('a', 'd', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).toList());
        assertEquals(N.toList('d', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).skip(1).toList());
        assertEquals(6, CharStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, CharStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'd', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList('d', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlattenWithAlignment() {
        char[][] array = new char[][] { { 'a', 'b' }, { 'c', 'd', 'e' } };
        assertEquals(6, CharStream.flatten(array, '*', false).count());
        assertEquals(5, CharStream.flatten(array, '*', false).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).toArray());
        assertArrayEquals(new char[] { 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).skip(1).toArray());
        assertEquals(N.toList('a', 'b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).toList());
        assertEquals(N.toList('b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).skip(1).toList());
        assertEquals(6, CharStream.flatten(array, '*', false).map(e -> e).count());
        assertEquals(5, CharStream.flatten(array, '*', false).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).map(e -> e).toList());
        assertEquals(N.toList('b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByGenerate() {
        final int[] count = { 0 };
        assertEquals(5, CharStream.generate(() -> (char) ++count[0]).limit(5).count());
        count[0] = 0;
        assertEquals(4, CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).limit(5).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).limit(5).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).toList());
        count[0] = 0;
        assertEquals(5, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).count());
        count[0] = 0;
        assertEquals(4, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testIfEmpty() {
        CharStream stream = CharStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        CharStream result = stream.ifEmpty(() -> called.set(true));
        result.count(); // terminal op to trigger
        assertTrue(called.get());
    }

    @Test
    public void testIfEmpty_NonEmpty() {
        CharStream stream = createCharStream('a', 'b');
        AtomicBoolean called = new AtomicBoolean(false);
        char[] result = stream.ifEmpty(() -> called.set(true)).toArray();
        assertFalse(called.get());
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().skip(1).count());
        Object[] indexed = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().toArray();
        assertEquals(5, indexed.length);
        indexed = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().skip(1).toArray();
        assertEquals(4, indexed.length);
        List<IndexedChar> indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals((char) 1, indexedList.get(0).value());
        indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals((char) 1, indexedList.get(0).value());
        indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).indexed().skip(1).toList();
        assertEquals(4, indexedList.size());
        assertEquals(1, indexedList.get(0).index());
        assertEquals((char) 2, indexedList.get(0).value());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        List<Character> collection = N.toList((char) 2, (char) 3, (char) 4, (char) 6);
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).toArray());
        assertArrayEquals(new char[] { 3, 4 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).toList());
        assertEquals(N.toList((char) 3, (char) 4), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).toArray());
        assertArrayEquals(new char[] { 3, 4 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).toList());
        assertEquals(N.toList((char) 3, (char) 4),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).toList());
    }

    @Test
    public void testIsParallel() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.isParallel());
    }

    // iterate

    @Test
    public void testStreamCreatedByIterateInfinite() {
        assertEquals(5, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).toList());
        assertEquals(5, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateWithBooleanSupplier() {
        final int[] count = { 0 };
        assertEquals(5, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).count());
        count[0] = 0;
        assertEquals(4, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).toList());
        count[0] = 0;
        assertEquals(5, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).count());
        count[0] = 0;
        assertEquals(4, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateWithInitAndPredicate() {
        assertEquals(5, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).toList());
        assertEquals(5, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testBooleanSupplierIteratorsStayExhausted() {
        final AtomicInteger conditionCalls = new AtomicInteger();
        CharIterator iter = CharStream.iterate(() -> conditionCalls.getAndIncrement() > 0, () -> 'a').iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextChar);
        assertEquals(1, conditionCalls.get());

        conditionCalls.set(0);
        iter = CharStream.iterate('a', () -> conditionCalls.getAndIncrement() > 0, value -> (char) (value + 1)).iterator();

        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextChar);
        assertEquals(1, conditionCalls.get());
    }

    @Test
    public void testIterator() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharIterator iterator = stream.iterator();
        assertTrue(iterator.hasNext());
        assertEquals('a', iterator.nextChar());
        assertEquals('b', iterator.nextChar());
        assertEquals('c', iterator.nextChar());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObjArray() {
        assertEquals(6, CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).toList());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).toList());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .count());
        assertEquals(1,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 20, 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toArray());
        assertArrayEquals(new char[] { 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 20, (char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toList());
        assertEquals(N.toList((char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toList());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .count());
        assertEquals(1,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 20, 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toArray());
        assertArrayEquals(new char[] { 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 20, (char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toList());
        assertEquals(N.toList((char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.toList(10, 20, 30, 40, 50), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).boxed().toList());
        assertEquals(N.toList(20, 30, 40, 50), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).boxed().toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.toList(10, 20, 30, 40, 50),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).boxed().toList());
        assertEquals(N.toList(20, 30, 40, 50),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).count());
        assertEquals(4, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).count());
        assertArrayEquals(new String[] { "C1", "C2", "C3", "C4", "C5" }, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).toArray(String[]::new));
        assertArrayEquals(new String[] { "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).toArray(String[]::new));
        assertEquals(N.toList("C1", "C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).toList());
        assertEquals(N.toList("C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).toList());
        assertEquals(5, CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).count());
        assertEquals(4, CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).count());
        assertArrayEquals(new String[] { "C1", "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).toArray(String[]::new));
        assertArrayEquals(new String[] { "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).toArray(String[]::new));
        assertEquals(N.toList("C1", "C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).toList());
        assertEquals(N.toList("C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        assertEquals(8,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(7,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(8,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(7,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedByOfNullable() {
        assertEquals(1, CharStream.ofNullable('a').count());
        assertEquals(0, CharStream.ofNullable('a').skip(1).count());
        assertArrayEquals(new char[] { 'a' }, CharStream.ofNullable('a').toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable('a').skip(1).toArray());
        assertEquals(N.toList('a'), CharStream.ofNullable('a').toList());
        assertEquals(N.toList(), CharStream.ofNullable('a').skip(1).toList());
        assertEquals(1, CharStream.ofNullable('a').map(e -> e).count());
        assertEquals(0, CharStream.ofNullable('a').map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a' }, CharStream.ofNullable('a').map(e -> e).toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable('a').map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a'), CharStream.ofNullable('a').map(e -> e).toList());
        assertEquals(N.toList(), CharStream.ofNullable('a').map(e -> e).skip(1).toList());

        assertEquals(0, CharStream.ofNullable((Character) null).count());
        assertEquals(0, CharStream.ofNullable((Character) null).skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) null).toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) null).skip(1).toArray());
        assertEquals(N.toList(), CharStream.ofNullable((Character) null).toList());
        assertEquals(N.toList(), CharStream.ofNullable((Character) null).skip(1).toList());
    }

    // prepend

    @Test
    public void testStreamCreatedAfterPrependArray() {
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        assertEquals(5, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).count());
        assertEquals(4, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).count());
        assertEquals(4, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toList());
    }

    @Test
    public void testPrintln() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertDoesNotThrow(() -> stream.println());
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> CharStream.empty().println());
    }

    @Test
    public void testPsp() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        char[] result = stream.psp(s -> s.filter(c -> c > 'b')).toArray();
        Arrays.sort(result);
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    // range

    @Test
    public void testInvalidRange() {
        assertDoesNotThrow(() -> {
            CharStream.range('z', 'a').toList().isEmpty();
        });
    }

    @Test
    public void testStreamCreatedByRange() {
        assertEquals(5, CharStream.range((char) 1, (char) 6).count());
        assertEquals(4, CharStream.range((char) 1, (char) 6).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).skip(1).toList());
        assertEquals(5, CharStream.range((char) 1, (char) 6).map(e -> e).count());
        assertEquals(4, CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeWithStep() {
        assertEquals(3, CharStream.range((char) 1, (char) 10, 3).count());
        assertEquals(2, CharStream.range((char) 1, (char) 10, 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7 }, CharStream.range((char) 1, (char) 10, 3).toArray());
        assertArrayEquals(new char[] { 4, 7 }, CharStream.range((char) 1, (char) 10, 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).toList());
        assertEquals(N.toList((char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).skip(1).toList());
        assertEquals(3, CharStream.range((char) 1, (char) 10, 3).map(e -> e).count());
        assertEquals(2, CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7 }, CharStream.range((char) 1, (char) 10, 3).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 7 }, CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).map(e -> e).toList());
        assertEquals(N.toList((char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testNegativeRepeat() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('a', -1));
    }

    @Test
    public void testStreamCreatedByRepeat() {
        assertEquals(5, CharStream.repeat('a', 5).count());
        assertEquals(4, CharStream.repeat('a', 5).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).toArray());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).skip(1).toArray());
        assertEquals(N.toList('a', 'a', 'a', 'a', 'a'), CharStream.repeat('a', 5).toList());
        assertEquals(N.toList('a', 'a', 'a', 'a'), CharStream.repeat('a', 5).skip(1).toList());
        assertEquals(5, CharStream.repeat('a', 5).map(e -> e).count());
        assertEquals(4, CharStream.repeat('a', 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'a', 'a', 'a', 'a'), CharStream.repeat('a', 5).map(e -> e).toList());
        assertEquals(N.toList('a', 'a', 'a', 'a'), CharStream.repeat('a', 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 },
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).count());
        assertArrayEquals(new char[] { 4, 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).toArray());
        assertArrayEquals(new char[] { 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).toList());
        assertEquals(N.toList((char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new char[] { 4, 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new char[] { 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList((char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).toList());
    }

    // scan

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).count());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 10, 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 10, (char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).count());
        assertEquals(5,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 10, 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 10, (char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).count());
        char[] shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().toArray();
        assertEquals(5, shuffled.length);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).toArray();
        assertEquals(4, shuffled.length);
        List<Character> shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().toList();
        assertEquals(5, shuffledList.size());
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).toList();
        assertEquals(4, shuffledList.size());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).count());
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().toArray();
        assertEquals(5, shuffled.length);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).toArray();
        assertEquals(4, shuffled.length);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().toList();
        assertEquals(5, shuffledList.size());
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).toList();
        assertEquals(4, shuffledList.size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        char[] shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).toArray();
        assertEquals(4, shuffled.length);
        rnd = new Random(42);
        List<Character> shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).toList();
        assertEquals(4, shuffledList.size());
        rnd = new Random(42);
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).toArray();
        assertEquals(4, shuffled.length);
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).toList();
        assertEquals(4, shuffledList.size());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        List<Character> skipped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).toList());
        skipped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).toArray());
        assertArrayEquals(new char[] { 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).toList());
        assertEquals(N.toList((char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).toArray());
        assertArrayEquals(new char[] { 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).toList());
        assertEquals(N.toList((char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        List<Character> collected = new ArrayList<>();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).toList());
        collected.clear();
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).toList());
        collected.clear();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).toList());
        collected.clear();
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfArray() {
        assertEquals(0, CharStream.empty().count());
        assertEquals(0, CharStream.empty().skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfArrayWithRange() {
        char[] array = new char[] { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, CharStream.of(array, 2, 5).count());
        assertEquals(2, CharStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of(array, 2, 5).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of(array, 2, 5).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharacterArray() {
        Character[] array = new Character[] { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(5, CharStream.of(array).count());
        assertEquals(4, CharStream.of(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(array).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(array).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(array).skip(1).toList());
        assertEquals(5, CharStream.of(array).map(e -> e).count());
        assertEquals(4, CharStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(array).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharacterArrayWithRange() {
        Character[] array = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' };
        assertEquals(3, CharStream.of(array, 2, 5).count());
        assertEquals(2, CharStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of(array, 2, 5).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of(array, 2, 5).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(5, CharStream.of(buffer).count());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(4, CharStream.of(buffer).skip(1).count());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(buffer).toArray());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(buffer).skip(1).toArray());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(buffer).toList());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(buffer).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharSequence() {
        assertEquals(5, CharStream.of("abcde").count());
        assertEquals(4, CharStream.of("abcde").skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of("abcde").toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of("abcde").skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of("abcde").toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of("abcde").skip(1).toList());
        assertEquals(5, CharStream.of("abcde").map(e -> e).count());
        assertEquals(4, CharStream.of("abcde").map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of("abcde").map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of("abcde").map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of("abcde").map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of("abcde").map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharSequenceWithRange() {
        assertEquals(3, CharStream.of("abcdefg", 2, 5).count());
        assertEquals(2, CharStream.of("abcdefg", 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of("abcdefg", 2, 5).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of("abcdefg", 2, 5).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of("abcdefg", 2, 5).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of("abcdefg", 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of("abcdefg", 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of("abcdefg", 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of("abcdefg", 2, 5).map(e -> e).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCollection() {
        List<Character> collection = N.toList('a', 'b', 'c', 'd', 'e');
        assertEquals(5, CharStream.of(collection).count());
        assertEquals(4, CharStream.of(collection).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(collection).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(collection).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(collection).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(collection).skip(1).toList());
        assertEquals(5, CharStream.of(collection).map(e -> e).count());
        assertEquals(4, CharStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(collection).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(collection).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfIterator() {
        CharIterator iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertEquals(5, CharStream.of(iterator).count());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertEquals(4, CharStream.of(iterator).skip(1).count());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(iterator).toArray());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(iterator).skip(1).toArray());
    }

    @Test
    public void testStreamReuseException() {
        CharStream stream = createCharStream('a', 'b', 'c');
        stream.count();

        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testStreamReuseThrowsException() {
        CharStream stream = createCharStream('a', 'b', 'c');
        stream.toArray(); // consume
        assertThrows(IllegalStateException.class, () -> stream.toArray());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 2 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    // throwIfEmpty

    @Test
    public void testThrowIfEmpty_Empty() {
        CharStream stream = CharStream.empty();
        assertThrows(NoSuchElementException.class, () -> stream.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        CharStream stream = createCharStream('a', 'b');
        char[] result = stream.throwIfEmpty().toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testThrowIfEmpty_WithCustomException() {
        CharStream stream = CharStream.empty();
        assertThrows(IllegalStateException.class, () -> stream.throwIfEmpty(() -> new IllegalStateException("custom")).toArray());
    }

    // zipWith

    @Test
    public void testStreamCreatedAfterZipWith() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 5, 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThree() {
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThreeDefaults() {
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
    }

    // ---- deep review 2026-11-02 G121 begin ----
    private static final char[] G121_SAMPLES = { 0, 'a', 1, Character.MIN_VALUE, Character.MAX_VALUE };
    private static final char G121_PAD_A = (char) -1;
    private static final char G121_PAD_B = (char) 9;
    private static final char G121_PAD_C = (char) 7;

    private static char[] g121Of(final int... values) {
        final char[] result = new char[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = (char) values[i];
        }

        return result;
    }

    private static char[] g121RandomArray(final java.util.Random rnd, final int maxLength) {
        final char[] a = new char[rnd.nextInt(maxLength + 1)];

        for (int i = 0; i < a.length; i++) {
            a[i] = rnd.nextInt(4) == 0 ? G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)] : (char) rnd.nextInt(1000);
        }

        return a;
    }

    private static char[] g121Drain(final com.landawn.abacus.util.CharIterator iterator) {
        final com.landawn.abacus.util.CharList list = new com.landawn.abacus.util.CharList();

        while (iterator.hasNext()) {
            list.add(iterator.nextChar());
        }

        return list.toArray();
    }

    private static char[] g121Suffix(final char[] a, final int from) {
        return from >= a.length ? new char[0] : java.util.Arrays.copyOfRange(a, from, a.length);
    }

    // variant 0: zip(a, b), 1: zip(a, b, c), 2: zip(a, b, padA, padB), 3: zip(a, b, c, padA, padB, padC); every call is recorded
    private static CharStream g121Zip(final int variant, final char[] a, final char[] b, final char[] c, final java.util.List<String> calls) {
        switch (variant) {
            case 0:
                return CharStream.zip(a, b, (x, y) -> {
                    calls.add(x + "," + y);
                    return (char) (x * 3 + y);
                });
            case 1:
                return CharStream.zip(a, b, c, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (char) (x * 3 + y - z);
                });
            case 2:
                return CharStream.zip(a, b, G121_PAD_A, G121_PAD_B, (x, y) -> {
                    calls.add(x + "," + y);
                    return (char) (x * 3 + y);
                });
            default:
                return CharStream.zip(a, b, c, G121_PAD_A, G121_PAD_B, G121_PAD_C, (x, y, z) -> {
                    calls.add(x + "," + y + "," + z);
                    return (char) (x * 3 + y - z);
                });
        }
    }

    private static char[] g121ZipReference(final int variant, final char[] a, final char[] b, final char[] c) {
        final int len = variant == 0 ? Math.min(a.length, b.length)
                : variant == 1 ? Math.min(a.length, Math.min(b.length, c.length))
                        : variant == 2 ? Math.max(a.length, b.length) : Math.max(a.length, Math.max(b.length, c.length));
        final char[] result = new char[len];

        for (int i = 0; i < len; i++) {
            final char x = i < a.length ? a[i] : G121_PAD_A;
            final char y = i < b.length ? b[i] : G121_PAD_B;
            final char z = i < c.length ? c[i] : G121_PAD_C;
            result[i] = variant == 0 || variant == 2 ? (char) (x * 3 + y) : (char) (x * 3 + y - z);
        }

        return result;
    }

    // G121-01: zip(char[]...) toArray()/toCharList()/skip(k).toArray()/iterator().toArray()|toList()/sorted() fill an exactly sized
    // array; values and the zipFunction call sequence equal the element-by-element traversal (pin: GREEN on base and fix)
    @org.junit.jupiter.api.Test
    public void testZipArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(121);

        for (int round = 0; round < 200; round++) {
            final char[] a = g121RandomArray(rnd, 12);
            final char[] b = g121RandomArray(rnd, 12);
            final char[] c = g121RandomArray(rnd, 12);

            for (int variant = 0; variant < 4; variant++) {
                final char[] expected = g121ZipReference(variant, a, b, c);
                final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Zip(variant, a, b, c, expectedCalls).iterator()));
                org.junit.jupiter.api.Assertions.assertEquals(expected.length, expectedCalls.size());

                final java.util.List<String> calls = new java.util.ArrayList<>();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Zip(variant, a, b, c, calls).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.CharList list = g121Zip(variant, a, b, c, calls).toCharList();
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                list.add((char) 42);
                org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

                final int k = rnd.nextInt(expected.length + 2);
                calls.clear();
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Zip(variant, a, b, c, calls).skip(k).toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

                calls.clear();
                final com.landawn.abacus.util.CharIterator iter = g121Zip(variant, a, b, c, calls).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int i = 0; i < consumed; i++) {
                    iter.nextChar();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, iter.toArray());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());

                calls.clear();
                final char[] sorted = expected.clone();
                java.util.Arrays.sort(sorted);
                org.junit.jupiter.api.Assertions.assertArrayEquals(sorted, g121Zip(variant, a, b, c, calls).sorted().toArray());
                org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            }
        }
    }

    private static boolean g121TakeFirst(final boolean stateful, final char x, final char y, final java.util.List<String> calls) {
        calls.add(x + "," + y);
        return stateful ? calls.size() % 3 != 0 : x <= y;
    }

    private static CharStream g121Merge(final char[] a, final char[] b, final boolean stateful, final java.util.List<String> calls) {
        return CharStream.merge(a, b, (x, y) -> g121TakeFirst(stateful, x, y, calls) ? com.landawn.abacus.util.MergeResult.TAKE_FIRST
                : com.landawn.abacus.util.MergeResult.TAKE_SECOND);
    }

    // G121-01: merge(char[], char[]) toArray()/toCharList() fill an exactly sized array; values and the order and number of
    // nextSelector calls (a stateful selector makes the result depend on them) equal the element-by-element traversal (pin)
    @org.junit.jupiter.api.Test
    public void testMergeArrays_bulkTerminalsMatchElementwiseTraversal() {
        final java.util.Random rnd = new java.util.Random(12103);

        for (int round = 0; round < 200; round++) {
            final char[] a = g121RandomArray(rnd, 10);
            final char[] b = g121RandomArray(rnd, 10);

            if (rnd.nextBoolean()) {
                java.util.Arrays.sort(a);
                java.util.Arrays.sort(b);
            }

            final boolean stateful = rnd.nextBoolean();
            final com.landawn.abacus.util.CharList expectedList = new com.landawn.abacus.util.CharList();
            final java.util.List<String> expectedCalls = new java.util.ArrayList<>();
            int i = 0;
            int j = 0;

            while (i < a.length || j < b.length) {
                if (i < a.length && (j >= b.length || g121TakeFirst(stateful, a[i], b[j], expectedCalls))) {
                    expectedList.add(a[i++]);
                } else {
                    expectedList.add(b[j++]);
                }
            }

            final char[] expected = expectedList.toArray();
            final java.util.List<String> calls = new java.util.ArrayList<>();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Merge(a, b, stateful, calls).iterator()));
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Merge(a, b, stateful, calls).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.CharList list = g121Merge(a, b, stateful, calls).toCharList();
            org.junit.jupiter.api.Assertions.assertArrayEquals(expected, list.toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            list.add((char) 1);
            org.junit.jupiter.api.Assertions.assertEquals(expected.length + 1, list.size());

            final int k = rnd.nextInt(expected.length + 2);
            calls.clear();
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Merge(a, b, stateful, calls).skip(k).toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);

            calls.clear();
            final com.landawn.abacus.util.CharIterator iter = g121Merge(a, b, stateful, calls).iterator();
            final int consumed = Math.min(k, expected.length);

            for (int n = 0; n < consumed; n++) {
                iter.nextChar();
            }

            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed), rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
            org.junit.jupiter.api.Assertions.assertEquals(expectedCalls, calls);
            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
            org.junit.jupiter.api.Assertions.assertEquals(0, iter.toArray().length);
        }
    }

    private static Object g121Bulk(final com.landawn.abacus.util.CharIterator iterator, final boolean useList) {
        return useList ? iterator.toList() : iterator.toArray();
    }

    // G121-01: a failing zipFunction or nextSelector inside toArray()/toList() leaves the iterator exactly where element-by-element
    // traversal leaves it (zip(a, b)/zip(a, b, c) advance before the call, the padded zips and merge do not) (pin)
    @org.junit.jupiter.api.Test
    public void testZipAndMergeArrays_failingCallbackKeepsIteratorState() {
        final char[] a = g121Of(1, 2, 3, 4, 5);
        final char[] b = g121Of(10, 20, 30, 40, 50, 60);
        final char[] c = g121Of(100, 200, 300, 400, 500, 600, 700);

        for (int viaList = 0; viaList < 2; viaList++) {
            final boolean useList = viaList == 1;
            final int[] calls = { 0 };

            final com.landawn.abacus.util.CharIterator zip2 = CharStream.zip(a, b, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("zip2");
                }

                return (char) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertEquals(3, calls[0]);
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(44, 55), g121Drain(zip2));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.CharIterator zip3 = CharStream.zip(a, b, c, (x, y, z) -> {
                if (++calls[0] == 2) {
                    throw new IllegalStateException("zip3");
                }

                return (char) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("zip3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(zip3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(333, 444, 555), g121Drain(zip3));
            org.junit.jupiter.api.Assertions.assertEquals(5, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.CharIterator pad2 = CharStream.zip(a, b, (char) -1, (char) -2, (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("pad2");
                }

                return (char) (x + y);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad2",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad2, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(33, 44, 55, 59), g121Drain(pad2));
            org.junit.jupiter.api.Assertions.assertEquals(7, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.CharIterator pad3 = CharStream.zip(a, b, c, (char) -1, (char) -2, (char) -3, (x, y, z) -> {
                if (++calls[0] == 6) {
                    throw new IllegalStateException("pad3");
                }

                return (char) (x + y + z);
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("pad3",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(pad3, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(659, 697), g121Drain(pad3));
            org.junit.jupiter.api.Assertions.assertEquals(8, calls[0]);

            calls[0] = 0;
            final com.landawn.abacus.util.CharIterator merge = CharStream.merge(g121Of(1, 3, 5), g121Of(2, 4, 6), (x, y) -> {
                if (++calls[0] == 3) {
                    throw new IllegalStateException("merge");
                }

                return x <= y ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND;
            }).iterator();
            org.junit.jupiter.api.Assertions.assertEquals("merge",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, () -> g121Bulk(merge, useList)).getMessage());
            org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(3, 4, 5, 6), g121Drain(merge));
            org.junit.jupiter.api.Assertions.assertEquals(6, calls[0]);
        }
    }

    // G121-01: a zipFunction that consumes its own stream's iterator re-entrantly (once) ends toArray()/toCharList() where the
    // hasNext()-driven loop ends, instead of failing with NoSuchElementException (pin)
    @org.junit.jupiter.api.Test
    public void testZipArrays_reentrantCallbackEndsBulkTerminalEarly() {
        final char[] a = g121Of(1, 2, 3, 4, 5);
        final char[] b = g121Of(10, 20, 30, 40, 50);
        final char[] c = g121Of(100, 200, 300, 400, 500);
        final Runnable[] consume = new Runnable[1];
        final boolean[] once = { true };

        CharStream s = CharStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (char) (x + y);
        });
        com.landawn.abacus.util.CharIterator it = s.iterator();
        consume[0] = it::nextChar;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), it.toArray());
        org.junit.jupiter.api.Assertions.assertFalse(it.hasNext());

        once[0] = true;
        s = CharStream.zip(a, b, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (char) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextChar;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(11, 33, 44, 55), s.toCharList().toArray());

        once[0] = true;
        s = CharStream.zip(a, b, c, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (char) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextChar;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(111, 333, 444, 555), it.toArray());

        once[0] = true;
        s = CharStream.zip(a, g121Of(7), (char) -1, (char) -2, (x, y) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (char) (x + y);
        });
        it = s.iterator();
        consume[0] = it::nextChar;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(8, 1, 2, 3), it.toArray());

        once[0] = true;
        s = CharStream.zip(a, g121Of(7), new char[0], (char) -1, (char) -2, (char) -3, (x, y, z) -> {
            if (once[0]) {
                once[0] = false;
                consume[0].run();
            }

            return (char) (x + y + z);
        });
        it = s.iterator();
        consume[0] = it::nextChar;
        org.junit.jupiter.api.Assertions.assertArrayEquals(g121Of(5, -2, -1, 0), it.toArray());
    }

    private static CharStream g121Flatten(final char[][] a, final int mode, final char pad) {
        return mode == 0 ? CharStream.flatten(a, true) : CharStream.flatten(a, pad, mode == 1);
    }

    // G121-01: flatten(char[][], true) (rectangular walk and the sparse jagged walk) and flatten(char[][], value, vertically)
    // toArray()/toCharList() fill an exactly sized array; pinned against plain column-major / row-major references, also after
    // skip(k) and partial iteration (pin)
    @org.junit.jupiter.api.Test
    public void testFlattenVerticalAndAligned_bulkTerminalsMatchReference() {
        final java.util.Random rnd = new java.util.Random(12102);

        for (int round = 0; round < 300; round++) {
            final int shape = rnd.nextInt(3);
            final int rows = shape == 2 ? 5 + rnd.nextInt(6) : rnd.nextInt(7);
            final char[][] a = new char[rows][];
            final int width = 1 + rnd.nextInt(6);

            for (int r = 0; r < rows; r++) {
                if (shape == 1) {
                    a[r] = new char[width];
                    java.util.Arrays.fill(a[r], (char) (r + 1));
                } else if (shape == 2) {
                    // one long row among null, empty and one-element rows: rows * maxLen > 4 * n selects the jagged walk
                    a[r] = r == rows / 2 ? g121RandomArray(rnd, 40) : rnd.nextInt(3) == 0 ? null : new char[rnd.nextInt(2)];
                } else {
                    a[r] = rnd.nextInt(5) == 0 ? null : g121RandomArray(rnd, 6);
                }
            }

            int maxLen = 0;

            for (final char[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final char pad = G121_SAMPLES[rnd.nextInt(G121_SAMPLES.length)];
            final com.landawn.abacus.util.CharList vertical = new com.landawn.abacus.util.CharList();
            final com.landawn.abacus.util.CharList alignedVertical = new com.landawn.abacus.util.CharList();
            final com.landawn.abacus.util.CharList alignedHorizontal = new com.landawn.abacus.util.CharList();

            for (int col = 0; col < maxLen; col++) {
                for (final char[] row : a) {
                    if (row != null && col < row.length) {
                        vertical.add(row[col]);
                        alignedVertical.add(row[col]);
                    } else {
                        alignedVertical.add(pad);
                    }
                }
            }

            for (final char[] row : a) {
                for (int col = 0; col < maxLen; col++) {
                    alignedHorizontal.add(row != null && col < row.length ? row[col] : pad);
                }
            }

            for (int mode = 0; mode < 3; mode++) {
                // a single row is returned as-is (no padding) by both flatten overloads
                final char[] expected = rows == 1 ? (a[0] == null ? new char[0] : a[0].clone())
                        : (mode == 0 ? vertical : mode == 1 ? alignedVertical : alignedHorizontal).toArray();

                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Flatten(a, mode, pad).toCharList().toArray());
                org.junit.jupiter.api.Assertions.assertArrayEquals(expected, g121Drain(g121Flatten(a, mode, pad).iterator()));

                final int k = rnd.nextInt(expected.length + 2);
                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, k), g121Flatten(a, mode, pad).skip(k).toArray());

                final com.landawn.abacus.util.CharIterator iter = g121Flatten(a, mode, pad).iterator();
                final int consumed = Math.min(k, expected.length);

                for (int n = 0; n < consumed; n++) {
                    iter.nextChar();
                }

                org.junit.jupiter.api.Assertions.assertArrayEquals(g121Suffix(expected, consumed),
                        rnd.nextBoolean() ? iter.toArray() : iter.toList().toArray());
                org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                org.junit.jupiter.api.Assertions.assertEquals(0, iter.toList().size());
                org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextChar);
            }
        }
    }

    // ---- deep review 2026-11-02 G121 end ----
    // ---- deep review 2026-11-02b G097 begin ----
    private static java.util.List<java.util.function.Supplier<CharStream>> g097RangeRepeatSources() {
        return java.util.Arrays.asList(() -> CharStream.range('a', 'b'), () -> CharStream.range('a', 'k'), () -> CharStream.range('a', 'l'),
                () -> CharStream.range((char) 0, (char) 0x400), () -> CharStream.range('a', 'z', 3), () -> CharStream.range('z', 'a', -4),
                () -> CharStream.range('a', 'b', 7), () -> CharStream.range((char) 0xFFF0, (char) 0xFFFF, 7),
                () -> CharStream.range('z', 'a', Integer.MIN_VALUE), () -> CharStream.rangeClosed('a', 'b'), () -> CharStream.rangeClosed('a', 'j'),
                () -> CharStream.rangeClosed((char) 0xFFF0, (char) 0xFFFF), () -> CharStream.rangeClosed((char) 0, (char) 0xFFFF),
                () -> CharStream.rangeClosed('a', 'z', 3), () -> CharStream.rangeClosed('z', 'a', -3),
                () -> CharStream.rangeClosed((char) 0xFFFF, (char) 0, -65535), () -> CharStream.rangeClosed('a', 'c', 10),
                () -> CharStream.rangeClosed((char) 0, (char) 0xFFFF, Integer.MAX_VALUE), () -> CharStream.repeat('x', 10),
                () -> CharStream.repeat((char) 0xFFFF, 11), () -> CharStream.repeat((char) 0, 1000));
    }

    private static char[] g097Drain(final CharIterator iterator) {
        final CharList list = new CharList();

        while (iterator.hasNext()) {
            list.add(iterator.nextChar());
        }

        return list.toArray();
    }

    private static char[] g097Suffix(final char[] a, final int from) {
        return Arrays.copyOfRange(a, from, a.length);
    }

    // G097-01 (pin): the range/rangeClosed/repeat iterators answer toList() (used by toCharList()) from their exact-size
    // toArray(); the result must equal element-wise traversal, for the whole stream and after a skip.
    @Test
    public void testRangeAndRepeat_toCharListMatchesElementwiseTraversal() {
        for (final java.util.function.Supplier<CharStream> source : g097RangeRepeatSources()) {
            final char[] expected = g097Drain(source.get().iterator());
            assertTrue(expected.length > 0);

            final CharList whole = source.get().toCharList();
            assertArrayEquals(expected, whole.toArray());
            whole.add('!'); // a fresh mutable list
            assertEquals(expected.length + 1, whole.size());
            assertEquals('!', whole.get(expected.length));

            for (final int k : new int[] { 1, 2, expected.length - 1, expected.length, expected.length + 1 }) {
                if (k >= 0) {
                    assertArrayEquals(g097Suffix(expected, Math.min(k, expected.length)), source.get().skip(k).toCharList().toArray());
                }
            }
        }
    }

    // G097-01 (pin): after a partial read, the iterator's toList() returns exactly the rest and leaves the iterator exhausted
    // (hasNext() false, a second toList()/count() empty, nextChar() throws NoSuchElementException), like element-wise traversal.
    @Test
    public void testRangeAndRepeat_iteratorToListAfterPartialReadLeavesIteratorExhausted() {
        for (final java.util.function.Supplier<CharStream> source : g097RangeRepeatSources()) {
            final char[] expected = g097Drain(source.get().iterator());

            for (final int k : new int[] { 0, 1, 2, expected.length - 1, expected.length }) {
                if (k < 0 || k > expected.length) {
                    continue;
                }

                final CharIterator it = source.get().iterator();

                for (int i = 0; i < k; i++) {
                    assertEquals(expected[i], it.nextChar());
                }

                final CharList rest = it.toList();
                assertArrayEquals(g097Suffix(expected, k), rest.toArray());
                assertFalse(it.hasNext());
                assertEquals(0, it.toList().size());
                assertEquals(0, ((CharIteratorEx) it).count());
                assertEquals(0, it.toArray().length);
                assertThrows(NoSuchElementException.class, it::nextChar);

                rest.add('!'); // a fresh mutable list
                assertEquals(expected.length - k + 1, rest.size());
            }
        }
    }

    // ---- deep review 2026-11-02b G097 end ----
    // ---- deep review 2026-11-02b G120 begin ----
    // G120-01 (pin): CharStream.of(CharBuffer) for a buffer without an accessible array (direct, read-only, sliced, view of a heap
    // ByteBuffer, wrapped String) reads the window [position, limit) through a dedicated iterator; toArray()/toList() fill an exact-size
    // array with the same get(i) reads. Results, laziness (live view), window snapshot and iterator states must match element-wise traversal.
    @Test
    public void testOfCharBuffer_nonArrayBufferTerminalsMatchElementwise_r116() {
        final char[] content = { 0x6B, 0xFFFF, 0, 'a', 'z', '5', '5', '3', 0xD800, 'B', '2', '1' };

        for (int kind = 0; kind < 6; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final char[] expected = Arrays.copyOfRange(content, from, to);
                    final java.nio.CharBuffer buf = g120CharBuffer(kind, content, from, to);

                    assertFalse(buf.hasArray());
                    assertArrayEquals(expected, CharStream.of(buf).toArray());
                    assertArrayEquals(expected, CharStream.of(buf).toCharList().toArray());
                    assertEquals(expected.length, CharStream.of(buf).count());
                    assertArrayEquals(g120CharSuffix(expected, 3), CharStream.of(buf).skip(3).toArray());
                    assertArrayEquals(g120CharSorted(expected), CharStream.of(buf).sorted().toArray());

                    final com.landawn.abacus.util.CharList list = CharStream.of(buf).toCharList();
                    list.add((char) 42); // a fresh, mutable list
                    assertEquals(expected.length + 1, list.size());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final CharIterator iter = CharStream.of(buf).iterator();

                        for (int n = 0; n < consumed; n++) {
                            assertEquals(expected[n], iter.nextChar());
                        }

                        final boolean viaArray = (consumed + from + kind) % 2 == 0;
                        final char[] rest = viaArray ? iter.toArray() : iter.toList().toArray();
                        assertArrayEquals(g120CharSuffix(expected, consumed), rest);
                        assertFalse(iter.hasNext());
                        assertEquals(0, iter.toList().size());
                        assertThrows(NoSuchElementException.class, iter::nextChar);

                        if (expected.length > 0) {
                            // an exhausted iterator returns the shared empty array, as the element-wise toArray() did
                            org.junit.jupiter.api.Assertions.assertSame(com.landawn.abacus.util.N.EMPTY_CHAR_ARRAY, iter.toArray());
                        } else {
                            assertEquals(0, iter.toArray().length);
                        }
                    }

                    final CharIteratorEx iterEx = (CharIteratorEx) CharStream.of(buf).iterator();
                    iterEx.advance(2);
                    assertArrayEquals(g120CharSuffix(expected, 2), iterEx.toArray());
                }
            }
        }

        // live view of the window taken at the call: later writes are seen, later position/limit changes are not
        final java.nio.CharBuffer direct = java.nio.ByteBuffer.allocateDirect(6 * 2).asCharBuffer();
        direct.put(new char[] { 1, 2, 3, 4, 5, 6 });
        direct.position(1);
        direct.limit(5);

        final CharStream stream = CharStream.of(direct);
        direct.put(4, (char) 40);
        direct.clear();
        direct.limit(0);

        assertArrayEquals(new char[] { 2, 3, 4, 40 }, stream.toArray());
        assertEquals(0, direct.position());
        assertEquals(0, direct.limit());
    }

    private static java.nio.CharBuffer g120CharBuffer(final int kind, final char[] content, final int from, final int to) {
        final java.nio.CharBuffer buf;

        if (kind == 0) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 2).asCharBuffer().put(content);
        } else if (kind == 1) {
            buf = java.nio.CharBuffer.wrap(content.clone()).asReadOnlyBuffer();
        } else if (kind == 2) {
            final java.nio.CharBuffer outer = java.nio.ByteBuffer.allocateDirect((content.length + 3) * 2).asCharBuffer();
            outer.position(2);
            outer.put(content);
            outer.position(2);
            outer.limit(2 + content.length);
            buf = outer.slice();
        } else if (kind == 3) {
            buf = java.nio.ByteBuffer.allocateDirect(content.length * 2).asCharBuffer().put(content).asReadOnlyBuffer();
        } else if (kind == 4) {
            buf = java.nio.ByteBuffer.allocate(content.length * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN).asCharBuffer().put(content);
        } else {
            buf = java.nio.CharBuffer.wrap(new String(content)); // read-only, no accessible array
        }

        buf.limit(to);
        buf.position(from);

        return buf;
    }

    private static char[] g120CharSuffix(final char[] a, final int k) {
        return Arrays.copyOfRange(a, Math.min(k, a.length), a.length);
    }

    private static char[] g120CharSorted(final char[] a) {
        final char[] copy = a.clone();
        Arrays.sort(copy);
        return copy;
    }

    // ---- deep review 2026-11-02b G120 end ----
    // ---- deep review 2026-11-02b G124 begin ----
    // G124: a buffer without an accessible array keeps the close-handler shape of the former
    // IntStream.range(position, limit).mapToChar(view::get) stream: exactly one (run-once, no-op) close handler.
    @Test
    public void testOfCharBuffer_nonArrayBufferKeepsRangeCloseHandler_r116() {
        final java.nio.CharBuffer direct = java.nio.ByteBuffer.allocateDirect(4 * 2).asCharBuffer();
        direct.put(new char[] { (char) 1, (char) 2, (char) 3, (char) 4 });
        direct.position(1);
        final java.nio.CharBuffer emptyWindow = direct.duplicate();
        emptyWindow.position(4);

        for (final java.nio.CharBuffer buf : java.util.List.of(direct, emptyWindow)) {
            // what iterator()'s "Remember to close" warning checks
            final CharStream stream = CharStream.of(buf);
            org.junit.jupiter.api.Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(stream.closeHandlers()));
            org.junit.jupiter.api.Assertions.assertEquals(1, stream.closeHandlers().size());

            // handlers registered later run once, in order; a failing one does not stop the others
            final java.util.List<String> calls = new java.util.ArrayList<>();
            final CharStream withHandlers = CharStream.of(buf).onClose(() -> calls.add("a")).onClose(() -> {
                calls.add("b");
                throw new IllegalStateException("b");
            }).onClose(() -> calls.add("c"));
            org.junit.jupiter.api.Assertions.assertEquals("b",
                    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::close).getMessage());
            withHandlers.close();
            org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of("a", "b", "c"), calls);
            org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, withHandlers::count);
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { (char) 2, (char) 3, (char) 4 }, CharStream.of(direct).toArray());
    }
    // ---- deep review 2026-11-02b G124 end ----

    // ---- re-review 2026-11-03 R04 begin ----
    // R04 (pin): CharStream.of(CharBuffer) for a buffer without an accessible array counts and skips by moving the cursor of its
    // window iterator instead of reading every skipped element. Pinned for every window, read position and skip length:
    // count(), advance(n) (negative, zero, inside, exactly to and past the end, Long.MAX_VALUE), the elements read after
    // it, the stream-level skip/step/count, the failure-atomic advance flag, and the live view of elements not yet read.
    @Test
    public void testOfCharBuffer_nonArrayBufferCountAndAdvanceMoveCursor_r116() {
        final char[] content = { 'a', 'b', 'Z', 'c', 'c', '0', ' ', 'x', 'y', 'q', 'r', '~' };

        for (int kind = 0; kind < 2; kind++) {
            for (int from = 0; from <= content.length; from++) {
                for (int to = from; to <= content.length; to++) {
                    final char[] expected = java.util.Arrays.copyOfRange(content, from, to);
                    final java.nio.CharBuffer buf = r04Buffer(kind, content, from, to);
                    org.junit.jupiter.api.Assertions.assertFalse(buf.hasArray());

                    org.junit.jupiter.api.Assertions.assertEquals(expected.length, CharStream.of(buf).count());

                    for (int k = 0; k <= expected.length + 1; k++) {
                        final int start = Math.min(k, expected.length);
                        final char[] rest = java.util.Arrays.copyOfRange(expected, start, expected.length);
                        org.junit.jupiter.api.Assertions.assertEquals(rest.length, CharStream.of(buf).skip(k).count());
                        org.junit.jupiter.api.Assertions.assertArrayEquals(rest, CharStream.of(buf).skip(k).toArray());
                    }

                    org.junit.jupiter.api.Assertions.assertArrayEquals(r04Step(expected, 3), CharStream.of(buf).step(3).toArray());

                    for (int consumed = 0; consumed <= expected.length; consumed++) {
                        final int remaining = expected.length - consumed;

                        for (final long n : new long[] { -1, 0, 1, 2, remaining, remaining + 1L, Long.MAX_VALUE }) {
                            final int next = consumed + (int) Math.min(Math.max(n, 0), remaining);
                            final CharIteratorEx iter = (CharIteratorEx) CharStream.of(buf).iterator();

                            if (expected.length > 0) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.supportsFailureAtomicAdvance());
                            }

                            for (int i = 0; i < consumed; i++) {
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextChar());
                            }

                            iter.advance(n);

                            for (int i = next; i < expected.length; i++) {
                                org.junit.jupiter.api.Assertions.assertTrue(iter.hasNext());
                                org.junit.jupiter.api.Assertions.assertEquals(expected[i], iter.nextChar());
                            }

                            org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
                            org.junit.jupiter.api.Assertions.assertEquals(0, iter.count());
                            org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class, iter::nextChar);

                            final CharIteratorEx counted = (CharIteratorEx) CharStream.of(buf).iterator();

                            for (int i = 0; i < consumed; i++) {
                                counted.nextChar();
                            }

                            counted.advance(n);
                            org.junit.jupiter.api.Assertions.assertEquals(expected.length - next, counted.count());
                            org.junit.jupiter.api.Assertions.assertFalse(counted.hasNext());
                            counted.advance(1);
                            org.junit.jupiter.api.Assertions.assertEquals(0, counted.count());
                        }
                    }
                }
            }
        }

        // A skip moves the cursor only: an element written after the skip is read live, a skipped one is never read.
        final java.nio.CharBuffer live = r04Buffer(1, content, 0, 6);
        final CharIteratorEx liveIter = (CharIteratorEx) CharStream.of(live).iterator();
        liveIter.advance(2);
        live.put(1, 'M');
        live.put(3, 'N');
        org.junit.jupiter.api.Assertions.assertEquals(content[2], liveIter.nextChar());
        org.junit.jupiter.api.Assertions.assertEquals('N', liveIter.nextChar());
        org.junit.jupiter.api.Assertions.assertEquals(2, liveIter.count());
        org.junit.jupiter.api.Assertions.assertFalse(liveIter.hasNext());
    }

    private static java.nio.CharBuffer r04Buffer(final int kind, final char[] content, final int from, final int to) {
        // kind 0: read-only view of a heap array; kind 1: writable view of direct memory. Neither exposes an array.
        final java.nio.CharBuffer buf = kind == 0 ? java.nio.CharBuffer.wrap(content.clone()).asReadOnlyBuffer()
                : java.nio.ByteBuffer.allocateDirect(content.length * 2).asCharBuffer().put(content).clear();
        buf.limit(to);
        buf.position(from);
        return buf;
    }

    private static char[] r04Step(final char[] a, final int step) {
        final char[] result = new char[(a.length + step - 1) / step];

        for (int i = 0; i < result.length; i++) {
            result[i] = a[i * step];
        }

        return result;
    }
    // ---- re-review 2026-11-03 R04 end ----
}
