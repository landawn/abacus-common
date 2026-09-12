package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;

import com.landawn.abacus.TestBase;

public abstract class CommonUtilTestSupport extends TestBase {

    enum DayOfWeek {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    protected static class TestBean {
        protected String name;
        protected String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    protected static class MyClass {
    }

    protected static class Person {
        protected final String name;
        protected final int age;

        protected Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }
    }

    protected static final class DatasetRowBean {
        protected String name;
        protected int age;

        public DatasetRowBean() {
        }

        protected DatasetRowBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    // --- Tests for checkArgNotEmpty (PrimitiveList, Multiset, Multimap, Dataset) ---

    // Tests for equals(Object, Object) with array types

    // Tests for equalsIgnoreCase(String[], fromIndexA, String[], fromIndexB, len) - same array ref

    // --- Tests for equalsInOrder ---

    // Tests for equalsInOrder(Map, Map)

    // --- Tests for equalsByCommonProps ---

    // Cover nested Iterable/Iterator/Map branches in hashCodeEverything.

    // Tests for hashCodeEverything

    // ===== equalsEverything (mirror/partner of hashCodeEverything) =====

    /** Asserts a==b deeply AND that the hashCodeEverything contract holds (equalsEverything(a,b) => equal hashes). */
    protected static void assertDeepEqualAndHash(Object a, Object b) {
        assertTrue(CommonUtil.equalsEverything(a, b), "expected equalsEverything(a, b) == true");
        assertEquals(CommonUtil.hashCodeEverything(a), CommonUtil.hashCodeEverything(b), "hashCodeEverything must agree when equalsEverything is true");
    }

    // --- Tests for size(PrimitiveList) ---

    // --- Tests for notEmpty (PrimitiveList, Multiset, Multimap, Dataset) ---

    // --- Tests for nullToEmpty with typed array ---

    // --- Tests for defaultValueOf with nonNullForPrimitiveWrapper ---

    // --- Tests for castIfAssignable with Type parameter ---

    // --- Tests for newProxyInstance ---

    // --- Tests for newHashMap with Collection and keyExtractor ---

    // --- Tests for newLinkedHashMap with Collection and keyExtractor ---

    // Tests for newDataset with columnNames, rows, properties

    // Tests for merge(Collection<Dataset>, boolean) - more scenarios

    // --- Tests for slice ---

    // Tests for toArray(Collection, int, int, IntFunction) with non-List collection

    // Tests for toArray(Collection, fromIndex, toIndex) with non-List path

    // Tests for toBooleanArray/toByteArray etc. with List (RandomAccess) path

    // --- Tests for toLinkedList ---

    // --- Tests for toLinkedHashSet ---

    // --- Tests for toSortedSet ---

    // --- Tests for toNavigableSet ---

    // --- Tests for toQueue, toDeque, toMultiset ---

    //

    // Tests for firstElements with Iterable (non-Collection)

    // Tests for containsSameElements for char, byte, short, long, float, double arrays

    // Tests for mismatch(Collection, fromIndexA, Collection, fromIndexB, len, keyExtractor)

    // Tests for rotate with range - char, short, long, float, double arrays

    // Tests for repeatElementsToSize with more complex collections

    // Tests for cycleToSize with truncation

    // Tests for copy with range (boolean, byte, char, short, int, long, float, double, Object)

    // Tests for copyOfRange(List, fromIndex, toIndex, step) edge cases

    // Tests for sort(List, fromIndex, toIndex, Comparator)

    // Tests for indexOf(Collection, Object, fromIndex)

    // Tests for lastIndexOf with startIndexFromBack parameter

    // Tests for indicesOfAll(Collection, Object, fromIndex)

    // Tests for indicesOfAll(Collection, Predicate, fromIndex)

    // ==================== Additional coverage tests (appended) ====================

    // Package-protected toString(StringBuilder, <primitive>[]) — null and empty branches for short/int/long/float/double.

    // unmodifiable* with an already-unmodifiable (Immutable) input returns the SAME instance (skips re-wrapping).

    // toArray(Collection) and toArray(Collection, a) empty-collection branches.

    // toArray(Collection, from, to, a) — non-List path that sets the trailing slot to null when a is oversized.

    // toCharArray(Collection, from, to, defaultForNull) — non-RandomAccess path with a null element.

    // newDataset(columnNames, rows): bean rows where a column name is missing -> null cell.

    // newDataset(columnNames, rows): array row whose length mismatches column count -> IllegalArgumentException.

    // newDataset(columnNames, rows): collection row whose size mismatches column count -> IllegalArgumentException.

    // newDataset(columnNames, rows): single column, scalar (non-array/collection/map/bean) row -> single-column case.

    // newDataset(columnNames, rows): multi-column with an unsupported scalar row -> IllegalArgumentException.

    // newDataset(columnNames, rows): null row produces a full row of nulls.

    // newDataset(rows): auto-derive columns from bean rows.

    // newDataset(rows): first element null -> IllegalArgumentException.

    // newDataset(rows): unsupported row type (scalar) -> IllegalArgumentException.

    // newDataset(keyColumnName, valueColumnName, map): non-empty map populates two columns.

    // newDataset(keyColumnName, valueColumnName, map): null/empty column names throw IAE.

    // newDataset(keyColumnName, valueColumnName, map): duplicate column names throw IAE.

    // newDataset(Map<String, Collection>): ragged columns are right-padded with null.

    // newSetMultimap(Map): seeds the multimap from a map's entries.

    // newSetMultimap(collection, keyExtractor): groups elements by extracted key.

    // newSetMultimap(collection, keyExtractor, valueExtractor): groups extracted values by extracted key.

    // hashCodeEverything(bean): recurses through bean properties.

    // findLastIndex(Collection, Predicate): non-RandomAccess, non-Deque collection -> toArray fall-through path.

    // lastNonNull(Iterable): non-Deque, non-list iterable -> delegates to iterator path.

    // convert: Clob -> String and Clob -> char[] (closes the clob via free()).

    // convert: Map -> Map copy path (same instance type recreated, contents preserved).

    // convert: Collection -> Collection copy path.

    // Minimal Clob stub backed by a String; only the methods exercised by convert(...) are implemented.
    protected static final class StubClob implements java.sql.Clob {
        protected final String data;
        boolean freed = false;

        StubClob(String data) {
            this.data = data;
        }

        @Override
        public long length() {
            return data.length();
        }

        @Override
        public String getSubString(long pos, int length) {
            return data.substring((int) (pos - 1), (int) (pos - 1) + length);
        }

        @Override
        public Reader getCharacterStream() {
            return new StringReader(data);
        }

        @Override
        public InputStream getAsciiStream() {
            return new ByteArrayInputStream(data.getBytes());
        }

        @Override
        public long position(String searchstr, long start) {
            return data.indexOf(searchstr, (int) (start - 1)) + 1;
        }

        @Override
        public long position(java.sql.Clob searchstr, long start) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setString(long pos, String str) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int setString(long pos, String str, int offset, int len) {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream setAsciiStream(long pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.Writer setCharacterStream(long pos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void truncate(long len) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void free() {
            freed = true;
        }

        @Override
        public Reader getCharacterStream(long pos, long length) {
            return new StringReader(data.substring((int) (pos - 1), (int) (pos - 1 + length)));
        }
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    public static class NewInstanceTopOuter {
        public class Mid {
            public class Leaf {
            }
        }
    }

    // mirrors java.util.HashMap.tableSizeFor
    protected static int hashMapTableSizeFor(final int cap) {
        final int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= (1 << 30)) ? (1 << 30) : n + 1;
    }

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
