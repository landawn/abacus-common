package com.landawn.abacus.util.stream;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fnn;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.stream.Stream.StreamEx;

public class NewStreamTest {

    @Test
    public void test_peek_last() throws Exception {
        Stream.of("a", "b", "c").peekLast(Fn.println()).forEach(Fn.println());
        Stream.of(N.asList("a", "b", "c").iterator()).peekLast(Fn.println()).forEach(Fn.println());
        Seq.of("a", "b", "c").peekLast(Fnn.println()).forEach(Fn.println());
    }

    @Test
    public void test_appendIfEmpty() {
        Stream.<String> empty().appendIfEmpty(() -> Stream.of("a", "b")).println();
        Stream.of("c", "d").appendIfEmpty(() -> Stream.of("a", "b")).println();
        Stream.<String> empty().peek(Fn.println()).appendIfEmpty(() -> Stream.of("a", "b")).println();
        Stream.of("c", "d").peek(Fn.println()).appendIfEmpty(() -> Stream.of("a", "b")).println();

        IntStream.empty().appendIfEmpty(() -> IntStream.of(1, 2)).println();
        IntStream.empty().onEach(N::println).appendIfEmpty(() -> IntStream.of(1, 2)).println();
        IntStream.of(3, 4).appendIfEmpty(() -> IntStream.of(1, 2)).println();
        IntStream.of(3, 4).onEach(N::println).appendIfEmpty(() -> IntStream.of(1, 2)).println();
    }

    @Test
    public void test_limit() {
        Stream.of(1, 2, 3).limit(100).forEach(Fn.println());
        Stream.of(1, 2, 3).map(i -> i).limit(100).forEach(Fn.println());

        Stream.of(1, 2, 3).skip(100).forEach(Fn.println());
        Stream.of(1, 2, 3).map(i -> i).skip(100).forEach(Fn.println());
    }

    @Test
    public void test_cartesianProduct() {

        StreamEx.<Object> of("a", "b", "c").cartesianProduct(N.asList("1", 2, 3), N.asList('x', 'y', 'z')).limit(5).forEach(Fn.println());
        N.println("----------------------");
        StreamEx.<Object> of("a", "b", "c").cartesianProduct(N.asList("1", 2, 3), N.asList('x', 'y', 'z')).skip(3).limit(5).forEach(Fn.println());
    }

}
