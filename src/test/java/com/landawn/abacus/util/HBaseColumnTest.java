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
        class Value implements Comparable<Value> {
            final int id;

            Value(int id) {
                this.id = id;
            }

            @Override
            public int compareTo(Value other) {
                return Integer.compare(id, other.id);
            }

            @Override
            public boolean equals(Object other) {
                return other instanceof Value value && id == value.id;
            }

            @Override
            public int hashCode() {
                return id;
            }

            @Override
            public String toString() {
                return "Value(" + id + ")";
            }
        }

        final HBaseColumn<Value> first = HBaseColumn.valueOf(new Value(1), 100L);
        final HBaseColumn<Value> equal = HBaseColumn.valueOf(new Value(1), 100L);
        final HBaseColumn<Value> other = HBaseColumn.valueOf(new Value(2), 100L);

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

    @Test
    public void testValueTieBreakIsDerivedFromTheValuesAndNotFromEncounterOrder() {
        class Opaque {
            // Not Comparable, equal only to itself, and every instance renders identically: nothing
            // reproducible tells two instances apart.
            @Override
            public int hashCode() {
                return 7;
            }

            @Override
            public String toString() {
                return "opaque";
            }
        }

        final HBaseColumn<Opaque> first = HBaseColumn.valueOf(new Opaque(), 100L);
        final HBaseColumn<Opaque> second = HBaseColumn.valueOf(new Opaque(), 100L);

        // The order must not fall back to a process-local sequence or identity hash code, which would make the
        // same data sort differently from one run to the next.
        Assertions.assertEquals(0, first.compareTo(second));
        Assertions.assertEquals(0, second.compareTo(first));
        Assertions.assertEquals(0, HBaseColumn.DESC_HBASE_COLUMN_COMPARATOR.compare(first, second));

        final TreeSet<HBaseColumn<Opaque>> set = new TreeSet<>();
        set.add(first);
        set.add(second);
        Assertions.assertEquals(1, set.size());

        // No interning table is consulted any more, so nothing is registered for a value.
        Assertions.assertTrue(java.util.Arrays.stream(HBaseColumn.class.getDeclaredFields())
                .noneMatch(f -> java.util.WeakHashMap.class.isAssignableFrom(f.getType())));
    }

    @Test
    public void testValueOrderDoesNotDependOnConstructionOrder() {
        final List<String> values = List.of("bb", "a", "ccc");
        final List<String> ascending = new ArrayList<>();
        final List<String> descending = new ArrayList<>();

        SortedSet<HBaseColumn<String>> set = new TreeSet<>();
        for (int i = 0; i < values.size(); i++) {
            set.add(HBaseColumn.valueOf(values.get(i), 100L));
        }
        set.forEach(column -> ascending.add(column.value()));

        set = new TreeSet<>();
        for (int i = values.size() - 1; i >= 0; i--) {
            set.add(HBaseColumn.valueOf(values.get(i), 100L));
        }
        set.forEach(column -> descending.add(column.value()));

        Assertions.assertEquals(List.of("a", "bb", "ccc"), ascending);
        Assertions.assertEquals(ascending, descending);
    }

    @Test
    public void testCompareToIsATotalOrderAcrossMixedValueTypes() {
        final List<HBaseColumn<Object>> columns = new ArrayList<>();

        for (Object value : new Object[] { null, "a", "b", 1, 2, 1L, 'c', 3.5d, List.of(1), Set.of(1) }) {
            columns.add(HBaseColumn.valueOf(value, 100L));
        }

        for (HBaseColumn<Object> x : columns) {
            for (HBaseColumn<Object> y : columns) {
                Assertions.assertEquals(-Integer.signum(y.compareTo(x)), Integer.signum(x.compareTo(y)));

                for (HBaseColumn<Object> z : columns) {
                    if (x.compareTo(y) < 0 && y.compareTo(z) < 0) {
                        Assertions.assertTrue(x.compareTo(z) < 0);
                    }

                    if (x.compareTo(y) == 0) {
                        Assertions.assertEquals(Integer.signum(x.compareTo(z)), Integer.signum(y.compareTo(z)));
                    }
                }
            }
        }

        // A TreeSet throws "Comparison method violates its general contract" on an inconsistent order.
        Assertions.assertEquals(columns.size(), new TreeSet<>(columns).size());
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
