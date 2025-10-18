package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IdentityHashSet100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        Assertions.assertTrue(set.isEmpty());
        Assertions.assertEquals(0, set.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        IdentityHashSet<String> set = new IdentityHashSet<>(100);
        Assertions.assertTrue(set.isEmpty());
        Assertions.assertEquals(0, set.size());
    }

    @Test
    public void testConstructorWithNegativeCapacity() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new IdentityHashSet<>(-1));
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> list = Arrays.asList("a", "b", "c");
        IdentityHashSet<String> set = new IdentityHashSet<>(list);
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.containsAll(list));
    }

    @Test
    public void testConstructorWithNullCollection() {
        Collection<String> nullCollection = null;
        IdentityHashSet<String> set = new IdentityHashSet<>(nullCollection);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testAdd() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = new String("hello");
        String s2 = new String("hello");

        Assertions.assertTrue(set.add(s1));
        Assertions.assertFalse(set.add(s1));
        Assertions.assertTrue(set.add(s2));

        Assertions.assertEquals(2, set.size());
    }

    @Test
    public void testAddNull() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        Assertions.assertTrue(set.add(null));
        Assertions.assertFalse(set.add(null));
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains(null));
    }

    @Test
    public void testRemove() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = new String("hello");
        String s2 = new String("hello");

        set.add(s1);
        set.add(s2);

        Assertions.assertTrue(set.remove(s1));
        Assertions.assertFalse(set.remove(s1));
        Assertions.assertEquals(1, set.size());

        Assertions.assertTrue(set.remove(s2));
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveWithDifferentReference() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = new String("hello");
        String s2 = new String("hello");

        set.add(s1);

        Assertions.assertFalse(set.remove(s2));
        Assertions.assertEquals(1, set.size());
    }

    @Test
    public void testContains() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = new String("hello");
        String s2 = new String("hello");

        set.add(s1);

        Assertions.assertTrue(set.contains(s1));
        Assertions.assertFalse(set.contains(s2));
    }

    @Test
    public void testContainsAll() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";
        String s3 = "c";

        set.add(s1);
        set.add(s2);

        Assertions.assertTrue(set.containsAll(Arrays.asList(s1, s2)));
        Assertions.assertFalse(set.containsAll(Arrays.asList(s1, s2, s3)));
        Assertions.assertTrue(set.containsAll(Collections.emptyList()));
    }

    @Test
    public void testAddAll() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";
        String s3 = "c";

        Assertions.assertTrue(set.addAll(Arrays.asList(s1, s2, s3)));
        Assertions.assertEquals(3, set.size());

        Assertions.assertFalse(set.addAll(Arrays.asList(s1, s2)));
        Assertions.assertFalse(set.addAll(Collections.emptyList()));
    }

    @Test
    public void testRemoveAll() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";
        String s3 = "c";

        set.addAll(Arrays.asList(s1, s2, s3));

        Assertions.assertTrue(set.removeAll(Arrays.asList(s1, s2)));
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains(s3));

        Assertions.assertFalse(set.removeAll(Collections.emptyList()));
    }

    @Test
    public void testRetainAll() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";
        String s3 = "c";

        set.addAll(Arrays.asList(s1, s2, s3));

        Assertions.assertTrue(set.retainAll(Arrays.asList(s1, s2)));
        Assertions.assertEquals(2, set.size());
        Assertions.assertTrue(set.contains(s1));
        Assertions.assertTrue(set.contains(s2));
        Assertions.assertFalse(set.contains(s3));
    }

    @Test
    public void testRetainAllEmpty() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        set.add("a");
        set.add("b");

        Assertions.assertTrue(set.retainAll(Collections.emptyList()));
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testRetainAllNoChange() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";

        set.add(s1);
        set.add(s2);

        Assertions.assertFalse(set.retainAll(Arrays.asList(s1, s2, "c")));
        Assertions.assertEquals(2, set.size());
    }

    @Test
    public void testIterator() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";
        String s3 = "c";

        set.addAll(Arrays.asList(s1, s2, s3));

        Set<String> collected = new HashSet<>();
        Iterator<String> iter = set.iterator();
        while (iter.hasNext()) {
            collected.add(iter.next());
        }

        Assertions.assertEquals(3, collected.size());
        Assertions.assertTrue(collected.contains(s1));
        Assertions.assertTrue(collected.contains(s2));
        Assertions.assertTrue(collected.contains(s3));
    }

    @Test
    public void testToArray() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";

        set.add(s1);
        set.add(s2);

        Object[] array = set.toArray();
        Assertions.assertEquals(2, array.length);
        Assertions.assertTrue(Arrays.asList(array).contains(s1));
        Assertions.assertTrue(Arrays.asList(array).contains(s2));
    }

    @Test
    public void testToArrayTyped() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        String s1 = "a";
        String s2 = "b";

        set.add(s1);
        set.add(s2);

        String[] array = set.toArray(new String[0]);
        Assertions.assertEquals(2, array.length);
        Assertions.assertTrue(Arrays.asList(array).contains(s1));
        Assertions.assertTrue(Arrays.asList(array).contains(s2));
    }

    @Test
    public void testToArrayTypedWithSufficientSize() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        set.add("a");
        set.add("b");

        String[] providedArray = new String[5];
        String[] result = set.toArray(providedArray);
        Assertions.assertSame(providedArray, result);
        Assertions.assertEquals(2, Arrays.stream(result).filter(Objects::nonNull).count());
    }

    @Test
    public void testSize() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        Assertions.assertEquals(0, set.size());

        set.add("a");
        Assertions.assertEquals(1, set.size());

        set.add("b");
        Assertions.assertEquals(2, set.size());

        set.remove("a");
        Assertions.assertEquals(1, set.size());
    }

    @Test
    public void testIsEmpty() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        Assertions.assertTrue(set.isEmpty());

        set.add("a");
        Assertions.assertFalse(set.isEmpty());

        set.clear();
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testClear() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        set.add("a");
        set.add("b");
        set.add("c");

        set.clear();
        Assertions.assertTrue(set.isEmpty());
        Assertions.assertEquals(0, set.size());
    }

    @Test
    public void testEquals() {
        IdentityHashSet<String> set1 = new IdentityHashSet<>();
        IdentityHashSet<String> set2 = new IdentityHashSet<>();

        String s = "hello";
        set1.add(s);
        set2.add(s);

        Assertions.assertTrue(set1.equals(set1));
        Assertions.assertTrue(set1.equals(set2));

        set2.add("world");
        Assertions.assertFalse(set1.equals(set2));

        Assertions.assertFalse(set1.equals(null));
        Assertions.assertFalse(set1.equals("not a set"));
        Assertions.assertFalse(set1.equals(new HashSet<>()));
    }

    @Test
    public void testHashCode() {
        IdentityHashSet<String> set1 = new IdentityHashSet<>();
        IdentityHashSet<String> set2 = new IdentityHashSet<>();

        String s = "hello";
        set1.add(s);
        set2.add(s);

        Assertions.assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    public void testToString() {
        IdentityHashSet<String> set = new IdentityHashSet<>();
        set.add("a");
        set.add("b");

        String str = set.toString();
        Assertions.assertTrue(str.startsWith("["));
        Assertions.assertTrue(str.endsWith("]"));
        Assertions.assertTrue(str.contains("a"));
        Assertions.assertTrue(str.contains("b"));
    }

    @Test
    public void testIdentitySemantics() {
        IdentityHashSet<Integer> set = new IdentityHashSet<>();

        Integer i1 = new Integer(128);
        Integer i2 = new Integer(128);

        set.add(i1);
        set.add(i2);

        Assertions.assertEquals(2, set.size());
        Assertions.assertTrue(set.contains(i1));
        Assertions.assertTrue(set.contains(i2));
        Assertions.assertFalse(set.contains(new Integer(128)));
    }

    @Test
    public void testWithMutableObjects() {
        IdentityHashSet<StringBuilder> set = new IdentityHashSet<>();

        StringBuilder sb1 = new StringBuilder("hello");
        StringBuilder sb2 = new StringBuilder("hello");

        set.add(sb1);
        set.add(sb2);

        Assertions.assertEquals(2, set.size());

        sb1.append(" world");
        Assertions.assertTrue(set.contains(sb1));
        Assertions.assertEquals(2, set.size());
    }
}
