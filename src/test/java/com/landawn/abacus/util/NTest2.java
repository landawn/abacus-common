package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XMLParser;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.util.stream.Stream;

public class NTest2 extends AbstractParserTest {

    static final XMLParser abacusXMLParser = ParserFactory.createAbacusXMLParser();

    //    @Test
    //    public void test_thread() throws Exception {
    //        int activeCount = Thread.activeCount();
    //        N.println(activeCount);
    //        List<Future<Integer>> futureList = new ArrayList<>();
    //
    //        for (int i = 0; i < 200; i++) {
    //            ContinuableFuture<Integer> f = N.asyncExecute(() -> {
    //                N.sleep(1000);
    //                return 1;
    //            });
    //
    //            futureList.add(f);
    //        }
    //
    //        assertEquals(200, Futures.allOf(futureList).get().stream().mapToInt(i -> i).sum());
    //
    //        int newActiveCount = Thread.activeCount();
    //        N.println(newActiveCount);
    //        assertTrue(newActiveCount - activeCount > 32);
    //    }

    @Test
    public void test_stringOf() {
        {
            final Boolean val = null;
            assertEquals(null, N.stringOf(val));
        }
        {
            final Integer val = null;
            assertEquals(null, N.stringOf(val));
        }
        {
            final Character val = null;
            assertEquals(null, N.stringOf(val));
        }
    }

    @Test
    public void test_sort_perf() {
        final int loopNum = 100;
        final int arrayLength = 10_000;

        final String[] a = new String[arrayLength];
        for (int i = 0; i < a.length; i++) {
            a[i] = Strings.uuid();
        }

        Profiler.run(1, loopNum, 3, "N.sort(...)", () -> N.sort(a.clone())).printResult();

        Profiler.run(1, loopNum, 3, "Arrays.sort(...)", () -> Arrays.sort(a.clone())).printResult();

        for (int i = 0; i < a.length; i++) {
            if (i % 3 == 0) {
                a[i] = null;
            }
        }

        Profiler.run(1, loopNum, 3, "Arrays.sort(.null.)", () -> Arrays.sort(a.clone())).printResult();

        Profiler.run(1, loopNum, 3, "N.sort(.null.)", () -> N.sort(a.clone())).printResult();
    }

    @Test
    public void test_lastElements() {
        final List<Integer> c = N.toList(Array.range(0, 1999));
        N.println(N.lastElements(c, 10));
        N.println(N.lastElements(c.iterator(), 10));

        assertEquals(c.subList(c.size() - 101, c.size()), N.lastElements(c, 101));
        assertEquals(c.subList(c.size() - 101, c.size()), N.lastElements(c.iterator(), 101));

        assertEquals(c.subList(0, 1099), N.firstElements(c, 1099));
        assertEquals(c.subList(0, 1099), N.firstElements(c.iterator(), 1099));
    }

    @Test
    public void test_skipRange() {
        {
            List<Integer> ret = N.skipRange(Stream.range(0, 7).toList(), 0, 3);
            assertEquals(ret, N.asList(3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toList(), 0, 0);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toList(), 5, 5);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toList(), 6, 6);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toList(), 7, 7);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toList(), 0, 7);
            assertEquals(ret, N.asList());

            ret = N.skipRange(Stream.range(0, 7).toList(), 0, 6);
            assertEquals(ret, N.asList(6));

            ret = N.skipRange(Stream.range(0, 7).toList(), 3, 6);
            assertEquals(ret, N.asList(0, 1, 2, 6));
        }

        {
            List<Integer> ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 0, 3);
            assertEquals(ret, N.asList(3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 0, 0);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 5, 5);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 6, 6);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 7, 7);
            assertEquals(ret, N.asList(0, 1, 2, 3, 4, 5, 6));

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 0, 7);
            assertEquals(ret, N.asList());

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 0, 6);
            assertEquals(ret, N.asList(6));

            ret = N.skipRange(Stream.range(0, 7).toCollection(Suppliers.ofLinkedHashSet()), 3, 6);
            assertEquals(ret, N.asList(0, 1, 2, 6));
        }
    }

    @Test
    public void test_runnable() {
        N.ifOrElse(true, Fnn.r(() -> N.println("abc")), Fn.r(() -> N.println(123)));
        N.ifOrElse(false, Fnn.r(() -> N.println("abc")), Fn.r(() -> N.println(123)));
        N.println(Strings.repeat('=', 80));

        N.println(Result.<Integer, Exception> of(1, null).orElseThrow(Fn.toRuntimeException()));
    }

    @Test
    public void test_formatJSON_formatXml() {
        final Account account = Beans.fill(Account.class);

        N.println(N.formatJson(N.toJson(account)));

        final Map m = N.fromJson(N.toJson(account), Map.class);
        N.println(m);

        final Map m2 = N.fromXml(N.toXml(account), Map.class);
        N.println(m2);

        N.println(N.toXml(m2, true));

        N.println(N.formatXml(N.toXml(account), MapEntity.class));

        N.println(N.formatXml(N.toXml(account), Account.class));
    }

    @Test
    public void test_indexOf() {
        {
            final long[] a = { 1, 2, 3, 4, 5, 6, 7 };
            assertEquals(3, N.indexOf(a, 4));
            assertEquals(4, N.lastIndexOf(a, 5));
        }

        {
            final List<Integer> list = N.asList(1, 2, 3, 4, 5, 6, 7);
            assertEquals(3, N.indexOf(list, 4));
            assertEquals(4, N.lastIndexOf(list, 5));
        }

        {
            final List<Integer> list = N.asLinkedList(1, 2, 3, 4, 5, 6, 7);
            assertEquals(3, N.indexOf(list, 4));
            assertEquals(4, N.lastIndexOf(list, 5));
            assertEquals(4, N.lastIndexOf(list, 5, 6));
            assertEquals(4, N.lastIndexOf(list, 5, 5));
            assertEquals(4, N.lastIndexOf(list, 5, 4));
            assertEquals(-1, N.lastIndexOf(list, 5, 3));
            assertEquals(-1, N.lastIndexOf(list, 5, 2));
        }

        {
            final Deque<Integer> list = N.asDeque(1, 2, 3, 4, 5, 6, 7);
            assertEquals(3, N.indexOf(list, 4));
            assertEquals(4, N.lastIndexOf(list, 5));
            assertEquals(4, N.lastIndexOf(list, 5, 6));
            assertEquals(4, N.lastIndexOf(list, 5, 5));
            assertEquals(4, N.lastIndexOf(list, 5, 4));
            assertEquals(-1, N.lastIndexOf(list, 5, 3));
            assertEquals(-1, N.lastIndexOf(list, 5, 2));
        }
    }

    @Test
    public void test_circularReference2() {
        final Object[] a = { "a1", null, "a3" };

        N.println(N.toJson(a));
        N.println(N.toXml(a));
        N.println(abacusXMLParser.serialize(a));

        N.println(N.fromJson(N.toJson(a), Object[].class));
        N.println(N.fromXml(N.toXml(a), Object[].class));
        N.println(N.fromXml(N.toXml(a), Object[].class));

        a[1] = a;

        try {
            N.println(N.toJson(a));
            fail("should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        try {
            N.println(N.toXml(a));
            fail("should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        try {
            N.println(abacusXMLParser.serialize(a));
            fail("should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        N.println(N.toJson(a, JSC.create().supportCircularReference(true)));
        N.println(N.toXml(a, XSC.create().supportCircularReference(true)));
        N.println(abacusXMLParser.serialize(a, XSC.create().supportCircularReference(true)));

        N.println(N.fromJson(N.toJson(a, JSC.create().supportCircularReference(true)), Object[].class));
        N.println(N.fromXml(N.toXml(a, XSC.create().supportCircularReference(true)), Object[].class));
        N.println(abacusXMLParser.deserialize(abacusXMLParser.serialize(a, XSC.create().supportCircularReference(true)), Object[].class));
    }

    @Test
    public void test_circularReference() {
        final AB ab = new AB();
        final BA ba = new BA();
        ba.setD("d");
        ab.setB(ba);
        ab.setC("c");

        N.println(N.toJson(ab));
        N.println(N.toXml(ab));
        N.println(abacusXMLParser.serialize(ab));

        N.println(N.fromJson(N.toJson(ab), AB.class));
        N.println(N.fromXml(N.toXml(ab), AB.class));
        N.println(abacusXMLParser.deserialize(abacusXMLParser.serialize(ab), AB.class));

        ba.setA(ab);

        try {
            N.println(N.toJson(ab));
            fail("should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        try {
            N.println(N.toXml(ab));
            fail("should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        try {
            N.println(abacusXMLParser.serialize(ab));
            fail("should throw StackOverflowError");
        } catch (final StackOverflowError e) {

        }

        N.println(N.toJson(ab, JSC.create().supportCircularReference(true)));
        N.println(N.toXml(ab, XSC.create().supportCircularReference(true)));
        N.println(abacusXMLParser.serialize(ab, XSC.create().supportCircularReference(true)));

        N.println(N.fromJson(N.toJson(ab, JSC.create().supportCircularReference(true)), AB.class));
        N.println(N.fromXml(N.toXml(ab, XSC.create().supportCircularReference(true)), AB.class));
        String xml = abacusXMLParser.serialize(ab, XSC.create().supportCircularReference(true));
        N.println(abacusXMLParser.deserialize(xml, AB.class));
    }

    public static class AB {

        public BA getB() {
            return b;
        }

        public void setB(final BA b) {
            this.b = b;
        }

        public String getC() {
            return c;
        }

        public void setC(final String c) {
            this.c = c;
        }

        private BA b;
        private String c;

    }

    public static class BA {

        public AB getA() {
            return a;
        }

        public void setA(final AB a) {
            this.a = a;
        }

        public String getD() {
            return d;
        }

        public void setD(final String d) {
            this.d = d;
        }

        private AB a;

        private String d;

    }

    @Test
    public void test_exclude() {
        final Set<Object> c = N.asSet("a", null, "b");
        final Set<Object> objsToExclude = N.asSet("a", null, 1, new Date());

        N.println(N.excludeAll(c, objsToExclude));

        N.println(N.exclude(c, null));
    }

    @Test
    public void test_commonElements() {
        Set<Integer> result = N.commonSet(N.asList(N.asList(1, 2, 3, 4)));
        N.println(result);

        result = N.commonSet(N.asList(1, 2, 3, 4), N.asList(1, 3, 5));
        N.println(result);

        result = N.commonSet(N.asSet(N.asList(1, 2, 3, 4), N.asList(1, 3, 5), N.asList(1, 2, 4)));
        N.println(result);

        result = N.commonSet(N.asSet(N.asList(1, 2, 3, 4), N.asList(1, 3, 5), N.asList(1, 2, 4), N.asSet(6)));
        N.println(result);

        result = N.commonSet(N.asList(N.asSet(6), N.asList(1, 2, 3, 4), N.asList(1, 3, 5), N.asList(1, 2, 4), N.asSet(6)));
        N.println(result);

        result = N.commonSet(N.repeat(N.asSet(6), 2));
        N.println(result);
    }

    @Test
    public void test_last() {
        assertEquals(3, N.lastElement(N.asLinkedList(1, 2, 3)).get().intValue());
        assertEquals(3, N.lastElement(N.asDeque(1, 2, 3)).get().intValue());
        assertEquals(3, N.lastNonNull(N.asLinkedList(1, 2, 3)).get().intValue());
        assertEquals(3, N.lastNonNull(N.asDeque(1, 2, 3)).get().intValue());
    }

    @Test
    public void test_compare() {
        assertEquals(0, N.compare(0d, 0d));
        assertEquals(0, N.compare(Double.valueOf(0d), Double.valueOf(0d)));

        assertEquals(0d, N.min(0d, 0d));
        assertEquals(0d, N.min(Double.valueOf(0d), Double.valueOf(0d)));

        assertEquals(0d, N.max(0d, 0d));
        assertEquals(0d, N.max(Double.valueOf(0d), Double.valueOf(0d)));

        assertEquals(0d, N.median(0d, 0d, 0d));
        assertEquals(0d, N.median(Double.valueOf(0d), Double.valueOf(0d), Double.valueOf(0d)));
    }

    @Test
    public void test_005() {
        final int[] a = { 0, 1, 2, 3, 4, 5 };
        final IntList intList = IntList.of(a);

        assertTrue(N.equals(Array.of(1, 2, 3, 4), intList.copy(1, 5).trimToSize().array()));
        assertTrue(N.equals(Array.of(1, 2, 3, 4), intList.copy(1, 5, 1).trimToSize().array()));
        assertTrue(N.equals(Array.of(1, 3), intList.copy(1, 5, 2).trimToSize().array()));
        assertTrue(N.equals(Array.of(1, 3, 5), intList.copy(1, 6, 2).trimToSize().array()));

        assertTrue(N.equals(Array.of(5, 4, 3, 2), intList.copy(5, 1, -1).trimToSize().array()));
        assertTrue(N.equals(Array.of(5, 3), intList.copy(5, 1, -2).trimToSize().array()));
        assertTrue(N.equals(Array.of(5, 3, 1), intList.copy(5, 0, -2).trimToSize().array()));
        assertTrue(N.equals(Array.of(5, 3, 1), intList.copy(5, -1, -2).trimToSize().array()));
        assertTrue(N.equals(Array.of(5, 4, 3, 2, 1, 0), intList.copy(5, -1, -1).trimToSize().array()));
        assertTrue(N.equals(Array.of(4, 2, 0), intList.copy(4, -1, -2).trimToSize().array()));
    }

    @Test
    public void test_004() {
        final int[] a = { 0, 1, 2, 3, 4, 5 };
        assertTrue(N.equals(Array.of(1, 2, 3, 4), N.copyOfRange(a, 1, 5)));
        assertTrue(N.equals(Array.of(1, 2, 3, 4), N.copyOfRange(a, 1, 5, 1)));
        assertTrue(N.equals(Array.of(1, 3), N.copyOfRange(a, 1, 5, 2)));
        assertTrue(N.equals(Array.of(1, 3, 5), N.copyOfRange(a, 1, 6, 2)));

        assertTrue(N.equals(Array.of(5, 4, 3, 2), N.copyOfRange(a, 5, 1, -1)));
        assertTrue(N.equals(Array.of(5, 3), N.copyOfRange(a, 5, 1, -2)));
        assertTrue(N.equals(Array.of(5, 3, 1), N.copyOfRange(a, 5, 0, -2)));
        assertTrue(N.equals(Array.of(5, 3, 1), N.copyOfRange(a, 6, 0, -2)));
        assertTrue(N.equals(Array.of(5, 3, 1), N.copyOfRange(a, 5, -1, -2)));
        assertTrue(N.equals(Array.of(5, 3, 1), N.copyOfRange(a, 6, -1, -2)));
        assertTrue(N.equals(Array.of(5, 4, 3, 2, 1, 0), N.copyOfRange(a, 5, -1, -1)));
        assertTrue(N.equals(Array.of(5, 4, 3, 2, 1, 0), N.copyOfRange(a, 6, -1, -1)));
        assertTrue(N.equals(Array.of(4, 2, 0), N.copyOfRange(a, 4, -1, -2)));
        assertTrue(N.equals(new int[0], N.copyOfRange(a, 6, 5, -1)));
        assertTrue(N.equals(new int[0], N.copyOfRange(a, 5, 5, -1)));
    }

    @Test
    public void test_002() {
        final Object[] a = { new String[] { "a", "b", "c" }, new int[] { 1, 2, 3 }, new Object[] { new char[] { 'e', 'f' } } };
        N.println(a);
        assertEquals("[[a, b, c], [1, 2, 3], [[e, f]]]", N.deepToString(a));

        for (final Set<String> set : Iterables.powerSet(N.asLinkedHashSet("a", "b", "c"))) {
            N.println(set);
        }
    }

    @Test
    public void test_001() {
        final Dataset dataset = N.newDataset(N.asList("contact.email"), N.asList(N.asList("addd")));
        dataset.println();

        final Account account = dataset.getRow(0, Account.class);
        N.println(account);

        final Account account2 = new Account();
        Beans.setPropValue(account2, "contact.email", "aaa");
        N.println(account2);
    }

}
