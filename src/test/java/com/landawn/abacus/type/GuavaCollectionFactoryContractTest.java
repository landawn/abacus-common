package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ForwardingMultimap;
import com.google.common.collect.ForwardingMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.landawn.abacus.TestBase;

/** Custom Guava implementations must be constructed through their advertised constructors or factories. */
public class GuavaCollectionFactoryContractTest extends TestBase {
    static java.util.stream.Stream<Arguments> maps() {
        return java.util.stream.Stream.of(Arguments.of(NoArgMap.class, false), Arguments.of(SizedMap.class, true), Arguments.of(FactoryMap.class, false),
                Arguments.of(SizedFactoryMap.class, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("maps")
    <M extends MapFixture> void customMultimapConstructionPreservesValues(Class<M> target, boolean sized) {
        GuavaMultimapType<String, Integer, M> type = new GuavaMultimapType<>(target, "String", "Integer");
        M result = type.valueOf("{\"a\":[1,2],\"b\":[3]}");
        assertEquals(target, result.getClass());
        assertEquals(List.of(1, 2), List.copyOf(result.get("a")));
        assertEquals(List.of(3), List.copyOf(result.get("b")));
        assertEquals(sized ? 2 : -1, result.requestedKeys);
        assertEquals(sized ? 1 : -1, result.requestedValues);
        assertEquals(result, type.valueOf(type.stringOf(result)));

        M empty = type.valueOf("{\"a\":null,\"b\":[]}");
        assertTrue(empty.isEmpty());
        assertEquals(sized ? 2 : -1, empty.requestedKeys);
        assertEquals(sized ? 0 : -1, empty.requestedValues);
    }

    static java.util.stream.Stream<Arguments> multisets() {
        return java.util.stream.Stream.of(Arguments.of(NoArgMultiset.class, false), Arguments.of(SizedMultiset.class, true),
                Arguments.of(FactoryMultiset.class, false), Arguments.of(SizedFactoryMultiset.class, true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("multisets")
    <M extends MultisetFixture> void customMultisetConstructionPreservesCounts(Class<M> target, boolean sized) {
        GuavaMultisetType<String, M> type = new GuavaMultisetType<>(target, "String");
        M result = type.valueOf("{\"a\":2,\"b\":3}");
        assertEquals(target, result.getClass());
        assertEquals(2, result.count("a"));
        assertEquals(3, result.count("b"));
        assertEquals(sized ? 2 : -1, result.requestedSize);
        assertEquals(result, type.valueOf(type.stringOf(result)));

        M empty = type.valueOf("{\"a\":null,\"b\":0}");
        assertTrue(empty.isEmpty());
        assertEquals(sized ? 2 : -1, empty.requestedSize);
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("{\"a\":-1}"));
    }

    static java.util.stream.Stream<Class<? extends MapFixture>> invalidMaps() {
        return java.util.stream.Stream.of(WrongReturnMap.class, InstanceFactoryMap.class, PrivateFactoryMap.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMaps")
    void rejectsUnusableMultimapFactories(Class<? extends MapFixture> target) {
        GuavaMultimapType<String, Integer, ?> type = new GuavaMultimapType<>(target, "String", "Integer");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> type.valueOf("{\"a\":[1]}"));
        assertTrue(failure.getMessage().contains(target.getName()));
    }

    static java.util.stream.Stream<Class<? extends MultisetFixture>> invalidMultisets() {
        return java.util.stream.Stream.of(WrongReturnMultiset.class, InstanceFactoryMultiset.class, PrivateFactoryMultiset.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidMultisets")
    void rejectsUnusableMultisetFactories(Class<? extends MultisetFixture> target) {
        GuavaMultisetType<String, ?> type = new GuavaMultisetType<>(target, "String");
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> type.valueOf("{\"a\":1}"));
        assertTrue(failure.getMessage().contains(target.getName()));
    }

    public abstract static class MapFixture extends ForwardingMultimap<String, Integer> {
        final int requestedKeys;
        final int requestedValues;
        private final Multimap<String, Integer> values = ArrayListMultimap.create();

        MapFixture(int keys, int valuesPerKey) {
            requestedKeys = keys;
            requestedValues = valuesPerKey;
        }

        @Override
        protected Multimap<String, Integer> delegate() {
            return values;
        }
    }

    public static final class NoArgMap extends MapFixture {
        public NoArgMap() {
            super(-1, -1);
        }
    }

    public static final class SizedMap extends MapFixture {
        public SizedMap(int keys, int values) {
            super(keys, values);
        }
    }

    public static final class FactoryMap extends MapFixture {
        private FactoryMap(boolean unused) {
            super(-1, -1);
        }

        public static FactoryMap create() {
            return new FactoryMap(true);
        }
    }

    public static final class SizedFactoryMap extends MapFixture {
        private SizedFactoryMap(int keys, int values, boolean unused) {
            super(keys, values);
        }

        public static SizedFactoryMap create(int keys, int values) {
            return new SizedFactoryMap(keys, values, true);
        }
    }

    public static final class WrongReturnMap extends MapFixture {
        private WrongReturnMap(boolean unused) {
            super(-1, -1);
        }

        public static Object create() {
            throw new AssertionError("invalid factory must not be called");
        }

        public static Object create(int keys, int values) {
            throw new AssertionError("invalid factory must not be called");
        }
    }

    public static final class InstanceFactoryMap extends MapFixture {
        private InstanceFactoryMap(boolean unused) {
            super(-1, -1);
        }

        public InstanceFactoryMap create() {
            throw new AssertionError("instance factory must not be called");
        }

        public InstanceFactoryMap create(int keys, int values) {
            throw new AssertionError("instance factory must not be called");
        }
    }

    public static final class PrivateFactoryMap extends MapFixture {
        private PrivateFactoryMap(boolean unused) {
            super(-1, -1);
        }

        private static PrivateFactoryMap create() {
            throw new AssertionError("private factory must not be called");
        }

        private static PrivateFactoryMap create(int keys, int values) {
            throw new AssertionError("private factory must not be called");
        }
    }

    public abstract static class MultisetFixture extends ForwardingMultiset<String> {
        final int requestedSize;
        private final Multiset<String> values = LinkedHashMultiset.create();

        MultisetFixture(int size) {
            requestedSize = size;
        }

        @Override
        protected Multiset<String> delegate() {
            return values;
        }
    }

    public static final class NoArgMultiset extends MultisetFixture {
        public NoArgMultiset() {
            super(-1);
        }
    }

    public static final class SizedMultiset extends MultisetFixture {
        public SizedMultiset(int size) {
            super(size);
        }
    }

    public static final class FactoryMultiset extends MultisetFixture {
        private FactoryMultiset(boolean unused) {
            super(-1);
        }

        public static FactoryMultiset create() {
            return new FactoryMultiset(true);
        }
    }

    public static final class SizedFactoryMultiset extends MultisetFixture {
        private SizedFactoryMultiset(int size, boolean unused) {
            super(size);
        }

        public static SizedFactoryMultiset create(int size) {
            return new SizedFactoryMultiset(size, true);
        }
    }

    public static final class WrongReturnMultiset extends MultisetFixture {
        private WrongReturnMultiset(boolean unused) {
            super(-1);
        }

        public static Object create() {
            throw new AssertionError("invalid factory must not be called");
        }

        public static Object create(int size) {
            throw new AssertionError("invalid factory must not be called");
        }
    }

    public static final class InstanceFactoryMultiset extends MultisetFixture {
        private InstanceFactoryMultiset(boolean unused) {
            super(-1);
        }

        public InstanceFactoryMultiset create() {
            throw new AssertionError("instance factory must not be called");
        }

        public InstanceFactoryMultiset create(int size) {
            throw new AssertionError("instance factory must not be called");
        }
    }

    public static final class PrivateFactoryMultiset extends MultisetFixture {
        private PrivateFactoryMultiset(boolean unused) {
            super(-1);
        }

        private static PrivateFactoryMultiset create() {
            throw new AssertionError("private factory must not be called");
        }

        private static PrivateFactoryMultiset create(int size) {
            throw new AssertionError("private factory must not be called");
        }
    }
}
