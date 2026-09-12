package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class HBaseColumnTest extends TestBase {

    @Test
    public void testEmptyOf() {
        HBaseColumn<Integer> empty = HBaseColumn.emptyOf(int.class);
        Assertions.assertEquals(0, empty.value());
        Assertions.assertEquals(0L, empty.version());

        HBaseColumn<String> emptyStr = HBaseColumn.emptyOf(String.class);
        Assertions.assertNull(emptyStr.value());
        Assertions.assertEquals(0L, emptyStr.version());
    }

    @Test
    public void testValueOfWithVersion() {
        HBaseColumn<String> col = HBaseColumn.valueOf("test", 12345L);
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
    }

    @Test
    public void testValueOf() {
        HBaseColumn<String> col = HBaseColumn.valueOf("test");
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(Long.MAX_VALUE, col.version());
    }

    @Test
    public void testAsList() {
        List<HBaseColumn<String>> list = HBaseColumn.asList("test");
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("test", list.get(0).value());
    }

    @Test
    public void testAsListWithVersion() {
        List<HBaseColumn<String>> list = HBaseColumn.asList("test", 12345L);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("test", list.get(0).value());
        Assertions.assertEquals(12345L, list.get(0).version());
    }

    @Test
    public void testAsSet() {
        Set<HBaseColumn<String>> set = HBaseColumn.asSet("test");
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.stream().anyMatch(col -> "test".equals(col.value())));
    }

    @Test
    public void testAsSetWithVersion() {
        Set<HBaseColumn<String>> set = HBaseColumn.asSet("test", 12345L);
        Assertions.assertEquals(1, set.size());
        HBaseColumn<String> col = set.iterator().next();
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
    }

    @Test
    public void testAsSortedSet() {
        SortedSet<HBaseColumn<String>> set = HBaseColumn.asSortedSet("test");
        Assertions.assertEquals(1, set.size());
        Assertions.assertEquals("test", set.first().value());
    }

    @Test
    public void testAsSortedSetWithComparator() {
        Comparator<HBaseColumn<?>> cmp = HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR;
        SortedSet<HBaseColumn<String>> set = HBaseColumn.asSortedSet("test", cmp);
        Assertions.assertEquals(1, set.size());
        Assertions.assertEquals("test", set.first().value());
    }

    @Test
    public void testDescComparatorKeepsSameVersionDifferentValues() {
        SortedSet<HBaseColumn<String>> set = new TreeSet<>(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR);

        set.add(HBaseColumn.valueOf("b", 10L));
        set.add(HBaseColumn.valueOf("a", 10L));

        Assertions.assertEquals(2, set.size());
    }

    @Test
    public void testAsSortedSetWithVersion() {
        SortedSet<HBaseColumn<String>> set = HBaseColumn.asSortedSet("test", 12345L);
        Assertions.assertEquals(1, set.size());
        HBaseColumn<String> col = set.first();
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
    }

    @Test
    public void testAsSortedSetWithVersionAndComparator() {
        Comparator<HBaseColumn<?>> cmp = HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR;
        SortedSet<HBaseColumn<String>> set = HBaseColumn.asSortedSet("test", 12345L, cmp);
        Assertions.assertEquals(1, set.size());
        HBaseColumn<String> col = set.first();
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
    }

    @Test
    public void testAsMap() {
        Map<Long, HBaseColumn<String>> map = HBaseColumn.asMap("test");
        Assertions.assertEquals(1, map.size());
        HBaseColumn<String> col = map.values().iterator().next();
        Assertions.assertEquals("test", col.value());
        Assertions.assertTrue(map.containsKey(col.version()));
    }

    @Test
    public void testAsMapWithVersion() {
        Map<Long, HBaseColumn<String>> map = HBaseColumn.asMap("test", 12345L);
        Assertions.assertEquals(1, map.size());
        Assertions.assertTrue(map.containsKey(12345L));
        Assertions.assertEquals("test", map.get(12345L).value());
    }

    @Test
    public void testAsSortedMap() {
        SortedMap<Long, HBaseColumn<String>> map = HBaseColumn.asSortedMap("test");
        Assertions.assertEquals(1, map.size());
        HBaseColumn<String> col = map.values().iterator().next();
        Assertions.assertEquals("test", col.value());
    }

    @Test
    public void testAsSortedMapWithComparator() {
        Comparator<Long> cmp = HBaseColumn.DESC_HBASE_VERSION_COMPARATOR;
        SortedMap<Long, HBaseColumn<String>> map = HBaseColumn.asSortedMap("test", cmp);
        Assertions.assertEquals(1, map.size());
        HBaseColumn<String> col = map.values().iterator().next();
        Assertions.assertEquals("test", col.value());
    }

    @Test
    public void testAsSortedMapWithVersion() {
        SortedMap<Long, HBaseColumn<String>> map = HBaseColumn.asSortedMap("test", 12345L);
        Assertions.assertEquals(1, map.size());
        Assertions.assertTrue(map.containsKey(12345L));
        Assertions.assertEquals("test", map.get(12345L).value());
    }

    @Test
    public void testAsSortedMapWithVersionAndComparator() {
        Comparator<Long> cmp = HBaseColumn.DESC_HBASE_VERSION_COMPARATOR;
        SortedMap<Long, HBaseColumn<String>> map = HBaseColumn.asSortedMap("test", 12345L, cmp);
        Assertions.assertEquals(1, map.size());
        Assertions.assertTrue(map.containsKey(12345L));
        Assertions.assertEquals("test", map.get(12345L).value());
    }

    @Test
    public void testConstructorWithValueAndVersion() {
        HBaseColumn<String> col = new HBaseColumn<>("test", 12345L);
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
    }

    @Test
    public void testValue() {
        HBaseColumn<Integer> col = new HBaseColumn<>(42, 12345L);
        Assertions.assertEquals(42, col.value());
    }

    @Test
    public void testConstructorWithValue() {
        HBaseColumn<String> col = new HBaseColumn<>("test");
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(Long.MAX_VALUE, col.version());
    }

    @Test
    public void testVersion() {
        HBaseColumn<String> col = new HBaseColumn<>("test", 12345L);
        Assertions.assertEquals(12345L, col.version());
    }

    @Test
    public void testCopy() {
        HBaseColumn<String> col = new HBaseColumn<>("test", 12345L);
        HBaseColumn<String> copy = col.copy();

        Assertions.assertNotSame(col, copy);
        Assertions.assertEquals(col.value(), copy.value());
        Assertions.assertEquals(col.version(), copy.version());
    }

    @Test
    public void testIsNull() {
        HBaseColumn<String> nullCol = new HBaseColumn<>(null, 0L);
        Assertions.assertTrue(nullCol.isNull());

        HBaseColumn<String> notNullCol = new HBaseColumn<>("test", 12345L);
        Assertions.assertFalse(notNullCol.isNull());

        Assertions.assertTrue(HBaseColumn.EMPTY_INT_COLUMN.isNull());
    }

    @Test
    public void testCompareTo() {
        HBaseColumn<String> col1 = new HBaseColumn<>("test1", 100L);
        HBaseColumn<String> col2 = new HBaseColumn<>("test2", 200L);

        Assertions.assertTrue(col1.compareTo(col2) < 0);
        Assertions.assertTrue(col2.compareTo(col1) > 0);
        Assertions.assertEquals(0, col1.compareTo(col1));
    }

    @Test
    public void testCompareToWithSameVersionDifferentValues() {
        HBaseColumn<String> col1 = new HBaseColumn<>("a", 100L);
        HBaseColumn<String> col2 = new HBaseColumn<>("b", 100L);

        Assertions.assertNotEquals(0, col1.compareTo(col2));
        Assertions.assertNotEquals(0, col2.compareTo(col1));
    }

    @Test
    public void testCompareToIsAntisymmetricForBaseAndSubclassValues() {
        class BaseValue implements Comparable<BaseValue> {
            @Override
            public int compareTo(BaseValue other) {
                return 1;
            }
        }

        class SubValue extends BaseValue {
            // Deliberately inherits BaseValue.compareTo. The old one-way isInstance check invoked
            // it for BaseValue -> SubValue but not for SubValue -> BaseValue.
        }

        HBaseColumn<BaseValue> base = new HBaseColumn<>(new BaseValue(), 100L);
        HBaseColumn<BaseValue> sub = new HBaseColumn<>(new SubValue(), 100L);

        int forward = Integer.signum(base.compareTo(sub));
        int reverse = Integer.signum(sub.compareTo(base));
        Assertions.assertNotEquals(0, forward);
        Assertions.assertEquals(-forward, reverse);
    }

    @Test
    public void testHashCode() {
        HBaseColumn<String> col1 = new HBaseColumn<>("test", 12345L);
        HBaseColumn<String> col2 = new HBaseColumn<>("test", 12345L);

        Assertions.assertEquals(col1.hashCode(), col2.hashCode());
    }

    @Test
    public void testEqualValuesHaveTheSameOrderRelativeToOtherValues() {
        class Value {
            final int id;
            final String label;

            Value(int id, String label) {
                this.id = id;
                this.label = label;
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof Value value && id == value.id;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return label;
            }
        }

        class SubValue extends Value {
            SubValue(int id, String label) {
                super(id, label);
            }
        }

        for (int scenario = 0; scenario < 3; scenario++) {
            final int id = scenario * 2;
            HBaseColumn<Value> first = HBaseColumn.valueOf(new Value(id, "a"), 100L);
            HBaseColumn<Value> other = HBaseColumn.valueOf(new Value(id + 1, scenario == 0 ? "a" : "m"), 100L);
            HBaseColumn<Value> equal = HBaseColumn.valueOf(scenario == 2 ? new SubValue(id, "z") : new Value(id, scenario == 0 ? "a" : "z"), 100L);

            for (Comparator<HBaseColumn<Value>> comparator : List.<Comparator<HBaseColumn<Value>>> of(Comparator.naturalOrder(),
                    HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR::compare)) {
                Assertions.assertEquals(0, comparator.compare(first, equal));
                Assertions.assertEquals(Integer.signum(comparator.compare(first, other)), Integer.signum(comparator.compare(equal, other)));
                Assertions.assertEquals(Integer.signum(comparator.compare(other, first)), Integer.signum(comparator.compare(other, equal)));

                SortedSet<HBaseColumn<Value>> columns = new TreeSet<>(comparator);
                columns.add(first);
                columns.add(other);
                Assertions.assertTrue(columns.contains(equal));
                Assertions.assertFalse(columns.add(equal));
                Assertions.assertEquals(2, columns.size());
            }
        }
    }

    @Test
    public void testReplacementValueOrderRetainsItsOwnWeakMapKey() throws Exception {
        class Value {
            final int id;

            Value(int id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof Value value && id == value.id;
            }

            @Override
            public int hashCode() {
                return id;
            }
        }

        final java.lang.reflect.Field registryField = HBaseColumn.class.getDeclaredField("valueOrders");
        registryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<Object, java.lang.ref.WeakReference<?>> registry = (Map<Object, java.lang.ref.WeakReference<?>>) registryField.get(null);
        final Value staleKey = new Value(1);
        final Value replacementKey = new Value(1);
        final Value otherKey = new Value(2);

        synchronized (registry) {
            // Simulate a collected representative with its older equal key still live,
            // without depending on the timing of a garbage collection.
            registry.put(staleKey, new java.lang.ref.WeakReference<>(null));
            try {
                final HBaseColumn<Value> replacement = HBaseColumn.valueOf(replacementKey, 1);
                final HBaseColumn<Value> other = HBaseColumn.valueOf(otherKey, 1);
                final HBaseColumn<Value> equal = HBaseColumn.valueOf(new Value(1), 1);
                final int sign = Integer.signum(replacement.compareTo(other));

                Assertions.assertSame(replacementKey, registry.keySet().stream().filter(replacementKey::equals).findFirst().orElseThrow());
                final Object representative = registry.get(replacementKey).get();
                Assertions.assertNotNull(representative);
                final java.lang.reflect.Field valueField = representative.getClass().getDeclaredField("value");
                valueField.setAccessible(true);
                Assertions.assertSame(replacementKey, valueField.get(representative));
                Assertions.assertEquals(0, replacement.compareTo(equal));
                Assertions.assertEquals(sign, Integer.signum(equal.compareTo(other)));
            } finally {
                registry.remove(replacementKey);
                registry.remove(otherKey);
            }
        }
    }

    @Test
    public void testEquals() {
        HBaseColumn<String> col1 = new HBaseColumn<>("test", 12345L);
        HBaseColumn<String> col2 = new HBaseColumn<>("test", 12345L);
        HBaseColumn<String> col3 = new HBaseColumn<>("other", 12345L);
        HBaseColumn<String> col4 = new HBaseColumn<>("test", 54321L);

        Assertions.assertEquals(col1, col2);
        Assertions.assertNotEquals(col1, col3);
        Assertions.assertNotEquals(col1, col4);
        Assertions.assertEquals(col1, col1);
        Assertions.assertNotEquals(col1, null);
        Assertions.assertNotEquals(col1, "string");
    }

    @Test
    public void testToString() {
        HBaseColumn<String> col = new HBaseColumn<>("test", 12345L);
        String str = col.toString();

        Assertions.assertTrue(str.contains("12345"));
        Assertions.assertTrue(str.contains("test"));
    }

    @Test
    public void testDescComparatorBreaksVersionTiesInAscendingValueOrder() {
        final HBaseColumn<String> a5 = HBaseColumn.valueOf("a", 5L);
        final HBaseColumn<String> b5 = HBaseColumn.valueOf("b", 5L);
        final HBaseColumn<String> c9 = HBaseColumn.valueOf("c", 9L);

        // Only the version term is reversed; the value tie-break stays the natural ascending order.
        Assertions.assertTrue(a5.compareTo(b5) < 0);
        Assertions.assertTrue(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR.compare(a5, b5) < 0);
        Assertions.assertTrue(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR.compare(b5, a5) > 0);
        Assertions.assertTrue(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR.compare(c9, a5) < 0);

        final TreeSet<HBaseColumn<?>> set = new TreeSet<>(HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR);
        set.add(a5);
        set.add(b5);
        set.add(c9);

        Assertions.assertEquals(List.of(c9, a5, b5), new ArrayList<HBaseColumn<?>>(set));
    }

    @Test
    public void testAsSortedSetDefaultOrderingIsVersionDescendingThenValueAscending() {
        final SortedSet<HBaseColumn<String>> set = HBaseColumn.asSortedSet("v", 1L);
        set.add(HBaseColumn.valueOf("a", 1L));
        set.add(HBaseColumn.valueOf("z", 9L));

        Assertions.assertEquals(List.of(HBaseColumn.valueOf("z", 9L), HBaseColumn.valueOf("a", 1L), HBaseColumn.valueOf("v", 1L)),
                new ArrayList<HBaseColumn<String>>(set));
    }
}
