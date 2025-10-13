package com.landawn.abacus.util;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class HBaseColumn100Test extends TestBase {

    @Test
    public void testConstructorWithValue() {
        HBaseColumn<String> col = new HBaseColumn<>("test");
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(Long.MAX_VALUE, col.version());
    }

    @Test
    public void testConstructorWithValueAndVersion() {
        HBaseColumn<String> col = new HBaseColumn<>("test", 12345L);
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
    }

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
    public void testValueOf() {
        HBaseColumn<String> col = HBaseColumn.valueOf("test");
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(Long.MAX_VALUE, col.version());
    }

    @Test
    public void testValueOfWithVersion() {
        HBaseColumn<String> col = HBaseColumn.valueOf("test", 12345L);
        Assertions.assertEquals("test", col.value());
        Assertions.assertEquals(12345L, col.version());
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
    public void testValue() {
        HBaseColumn<Integer> col = new HBaseColumn<>(42, 12345L);
        Assertions.assertEquals(42, col.value());
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
    public void testHashCode() {
        HBaseColumn<String> col1 = new HBaseColumn<>("test", 12345L);
        HBaseColumn<String> col2 = new HBaseColumn<>("test", 12345L);

        Assertions.assertEquals(col1.hashCode(), col2.hashCode());
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
}
