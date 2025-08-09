package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XMLParser;

import lombok.Builder;
import lombok.Data;

public class NTest3 {

    static final XMLParser abacusXMLParser = ParserFactory.createAbacusXMLParser();

    @Test
    public void test_forEach_2() {
        List<?> list = N.toList(Array.range(0, 12));

        N.forEach(list, e -> N.println(e + ": " + Thread.currentThread().getName()), 3);

        N.forEach(0, 10, 3, N::println);
        N.forEach(10, -3, -3, N::println);

        N.forEach(0, 10, N.asList("a", "b"), (i, c) -> N.println(i + ": " + c));
        N.forEach(10, -3, -3, N.asList("a", "b"), (i, c) -> N.println(i + ": " + c));
    }

    @Test
    public void test_stringOf() {
        {
            Long val = null;
            N.println(N.stringOf(val));
        }

    }

    @Test
    public void test_countBy() {
        List<String> c = N.asList("a", "b", "c", "a", "d", "E", "a", "b");

        N.countBy(c, Fn.identity()).forEach(Fn.println("="));
    }

    @Test
    public void test_deleteAllByIndices() {
        {
            List<Integer> c = N.asLinkedList(1, 2, 3, 4, 5, 6);

            N.deleteAllByIndices(c, 1, 3, 5);
            N.println(c);
            assertEquals(N.asLinkedList(1, 3, 5), c);
        }
        {
            List<Integer> c = N.asLinkedList(1, 2, 3, 4, 5, 6);

            N.deleteAllByIndices(c, 0, 2, 4);
            N.println(c);
            assertEquals(N.asLinkedList(2, 4, 6), c);
        }
        {
            List<Integer> c = N.asLinkedList(1, 2, 3, 4, 5, 6);

            N.deleteAllByIndices(c, 0, 1, 2, 4, 5);
            N.println(c);
            assertEquals(N.asLinkedList(4), c);
        }
        {
            List<Integer> c = N.asList(1, 2, 3, 4, 5, 6);

            N.deleteAllByIndices(c, 1, 3, 5);
            N.println(c);
            assertEquals(N.asList(1, 3, 5), c);
        }
        {
            List<Integer> c = N.asList(1, 2, 3, 4, 5, 6);

            N.deleteAllByIndices(c, 0, 2, 4);
            N.println(c);
            assertEquals(N.asList(2, 4, 6), c);
        }
        {
            List<Integer> c = N.asList(1, 2, 3, 4, 5, 6);

            N.deleteAllByIndices(c, 0, 1, 2, 4, 5);
            N.println(c);
            assertEquals(N.asList(4), c);
        }

    }

    @Test
    public void test_forEach() {
        {
            N.println("==================: LinkedHashSet");
            Collection<String> c = N.asLinkedHashSet("a", "b", "c", "d", "e", "f");
            Joiner joiner = Joiner.with(", ");
            N.forEach(c, 2, 5, it -> joiner.append(it));

            assertEquals("c, d, e", joiner.toString());
        }
        {
            N.println("==================: LinkedList");
            Collection<String> c = N.asLinkedList("a", "b", "c", "d", "e", "f");
            Joiner joiner = Joiner.with(", ");
            N.forEach(c, 2, 5, it -> joiner.append(it));

            assertEquals("c, d, e", joiner.toString());
        }
        {
            N.println("==================: List");
            Collection<String> c = N.asList("a", "b", "c", "d", "e", "f");
            Joiner joiner = Joiner.with(", ");
            N.forEach(c, 2, 5, it -> joiner.append(it));

            assertEquals("c, d, e", joiner.toString());
        }
        {
            N.println("==================: LinkedHashSet");
            Collection<String> c = N.asLinkedHashSet("a", "b", "c", "d", "e", "f");
            Joiner joiner = Joiner.with(", ");
            N.forEach(c, 5, 2, it -> joiner.append(it));

            assertEquals("f, e, d", joiner.toString());
        }
        {
            N.println("==================: LinkedList");
            Collection<String> c = N.asLinkedList("a", "b", "c", "d", "e", "f");
            Joiner joiner = Joiner.with(", ");
            N.forEach(c, 5, 2, it -> joiner.append(it));

            assertEquals("f, e, d", joiner.toString());
        }
        {
            N.println("==================: List");
            Collection<String> c = N.asList("a", "b", "c", "d", "e", "f");
            Joiner joiner = Joiner.with(", ");
            N.forEach(c, 5, 2, it -> joiner.append(it));

            assertEquals("f, e, d", joiner.toString());
        }
    }

    @Test
    public void copyEntitiesWithDiffFieldName() {
        A a = A.builder().fieldA1("aa").fieldA2(111).build();
        B b = Beans.copy(a, B.class);
        N.println(a);
        N.println(b);
    }

    @Builder
    @Data
    public static class A {
        @JsonXmlField(alias = { "fieldB1" })
        private String fieldA1;
        private int fieldA2;
    }

    @Builder
    @Data
    public static class B {
        private String fieldB1;
        @JsonXmlField(alias = { "fieldA2" })
        private int fieldB2;
    }

}
