package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.FieldSerializer;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;

/**
 * Verification (2026-10-27, G147) of the call-ordered Kryo registration replay (G010-01): built-in types whose ID is
 * taken by an explicit ID (unchanged from r108), the size of the registration logs, and registration racing the use of
 * the parser.
 */
public class KryoParserVerify20261027G147Test extends TestBase {

    public static class StringSlotTaker {
    }

    public static class StringSlotTakerById {
    }

    public static class ArrayListSlotTaker {
    }

    public static class ArrayListSlotTakerReversed {
    }

    public static class UnregisteredBuiltInSlotTaker {
    }

    public static class GlobalBuiltInSlotTaker {
    }

    public static class LogTargetLocal {
    }

    public static class LogTargetGlobal {
    }

    public static class Race00 {
    }

    public static class Race01 {
    }

    public static class Race02 {
    }

    public static class Race03 {
    }

    public static class Race04 {
    }

    public static class Race05 {
    }

    public static class Race06 {
    }

    public static class Race07 {
    }

    public static class Race08 {
    }

    public static class Race09 {
    }

    public static class Race10 {
    }

    public static class Race11 {
    }

    /** Prefixes every string it writes, so a string that comes back prefixed went through this serializer. */
    private static final class PrefixStringSerializer extends Serializer<String> {
        @Override
        public void write(final Kryo kryo, final Output output, final String value) {
            output.writeString("<p>" + value);
        }

        @Override
        public String read(final Kryo kryo, final Input input, final Class<? extends String> type) {
            return input.readString();
        }
    }

    private static int registrationIdOf(final Class<?> type) {
        final KryoParser baseline = new KryoParser();
        final Kryo kryo = baseline.createKryo();

        try {
            return kryo.getRegistration(type).getId();
        } finally {
            baseline.recycle(kryo);
        }
    }

    private static int firstFreeId() {
        final KryoParser baseline = new KryoParser();
        final Kryo kryo = baseline.createKryo();

        try {
            return kryo.getNextRegistrationId();
        } finally {
            baseline.recycle(kryo);
        }
    }

    private static Object field(final KryoParser parser, final String name) throws Exception {
        final Field field = KryoParser.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(parser);
    }

    @SuppressWarnings("unchecked")
    private static Set<Class<?>> registrationOrderOf(final KryoParser parser) throws Exception {
        return (Set<Class<?>>) field(parser, "kryoRegistrationOrder");
    }

    // a built-in type whose built-in ID an explicit ID of the same parser takes: a registration of the built-in type that is
    // applied before the taking one (register(Class, Serializer) before register(Class, Serializer, int)) is displaced, so
    // the type falls back to a name-based registration with the default serializer; one applied after it
    // (register(Class, Serializer) after register(Class, int)) gives it the next free ID with its serializer (as on r108)
    @Test
    public void testBuiltInTypeWhoseIdIsTakenKeepsItsRegistrationOnlyWhenAppliedAfterTheTakingOne() {
        final int stringId = registrationIdOf(String.class);
        final int firstFreeId = firstFreeId();
        final PrefixStringSerializer serializer = new PrefixStringSerializer();
        final KryoParser takenAfter = new KryoParser();
        takenAfter.register(String.class, serializer);
        takenAfter.register(StringSlotTaker.class, new FieldSerializer<>(new Kryo(), StringSlotTaker.class), stringId);
        final KryoParser takenBefore = new KryoParser();
        takenBefore.register(String.class, serializer);
        takenBefore.register(StringSlotTakerById.class, stringId);

        Kryo kryo = takenAfter.createKryo();

        try {
            assertEquals(stringId, kryo.getRegistration(StringSlotTaker.class).getId());
            assertNull(kryo.getClassResolver().getRegistration(String.class));
        } finally {
            takenAfter.recycle(kryo);
        }

        assertEquals("x", takenAfter.decode(takenAfter.encode("x")));
        assertSame(StringSlotTaker.class, takenAfter.decode(takenAfter.encode(new StringSlotTaker())).getClass());

        kryo = takenBefore.createKryo();

        try {
            assertEquals(stringId, kryo.getRegistration(StringSlotTakerById.class).getId());
            final Registration registration = kryo.getClassResolver().getRegistration(String.class);
            assertNotNull(registration);
            assertSame(serializer, registration.getSerializer());
            assertEquals(firstFreeId, registration.getId());
        } finally {
            takenBefore.recycle(kryo);
        }

        assertEquals("<p>x", takenBefore.decode(takenBefore.encode("x")));
    }

    // the same for register(Class) of a built-in type: it is applied before every register(Class, int), so in either call
    // order the built-in type is displaced and falls back to a name-based registration (as on r108)
    @Test
    public void testClassOnlyBuiltInTypeWhoseIdIsTakenFallsBackToNameBasedRegistrationInEitherCallOrder() {
        final int arrayListId = registrationIdOf(ArrayList.class);
        final KryoParser builtInFirst = new KryoParser();
        builtInFirst.register(ArrayList.class);
        builtInFirst.register(ArrayListSlotTaker.class, arrayListId);
        final KryoParser explicitFirst = new KryoParser();
        explicitFirst.register(ArrayListSlotTakerReversed.class, arrayListId);
        explicitFirst.register(ArrayList.class);

        for (final KryoParser parser : List.of(builtInFirst, explicitFirst)) {
            final Kryo kryo = parser.createKryo();

            try {
                assertNull(kryo.getClassResolver().getRegistration(ArrayList.class));
            } finally {
                parser.recycle(kryo);
            }

            final List<Object> list = new ArrayList<>(List.of(1, "a"));
            assertEquals(list, parser.decode(parser.encode(list)));
        }
    }

    // unchanged neighbours: an explicit ID taking the ID of a built-in type this parser does not register leaves that type
    // name-based, also when the type is registered globally; without such an explicit ID a built-in type keeps its ID
    @Test
    public void testBuiltInTypeNotRegisteredInOwnScopeFallsBackToNameBasedRegistration() {
        final int arrayListId = registrationIdOf(ArrayList.class);
        final int linkedListId = registrationIdOf(LinkedList.class);

        final KryoParser keepsBuiltInId = new KryoParser();
        keepsBuiltInId.register(ArrayList.class);
        Kryo kryo = keepsBuiltInId.createKryo();

        try {
            assertEquals(arrayListId, kryo.getRegistration(ArrayList.class).getId());
        } finally {
            keepsBuiltInId.recycle(kryo);
        }

        final KryoParser unregistered = new KryoParser();
        unregistered.register(UnregisteredBuiltInSlotTaker.class, arrayListId);
        kryo = unregistered.createKryo();

        try {
            assertEquals(arrayListId, kryo.getRegistration(UnregisteredBuiltInSlotTaker.class).getId());
            assertNull(kryo.getClassResolver().getRegistration(ArrayList.class));
        } finally {
            unregistered.recycle(kryo);
        }

        assertEquals(List.of("e"), unregistered.decode(unregistered.encode(new ArrayList<>(List.of("e")))));

        try {
            ParserFactory.registerKryo(LinkedList.class);
            final KryoParser globalBuiltIn = new KryoParser();
            globalBuiltIn.register(GlobalBuiltInSlotTaker.class, linkedListId);
            kryo = globalBuiltIn.createKryo();

            try {
                assertEquals(linkedListId, kryo.getRegistration(GlobalBuiltInSlotTaker.class).getId());
                assertNull(kryo.getClassResolver().getRegistration(LinkedList.class));
            } finally {
                globalBuiltIn.recycle(kryo);
            }

            assertEquals(new LinkedList<>(List.of("e")), globalBuiltIn.decode(globalBuiltIn.encode(new LinkedList<>(List.of("e")))));
        } finally {
            synchronized (ParserFactory._kryoRegistrationLock) {
                ParserFactory._kryoClassSet.remove(LinkedList.class);
                ParserFactory._kryoRegistrationVersion.incrementAndGet();
            }
        }
    }

    // re-registering one class through all four overloads keeps one entry in the registration log of each scope (removal
    // and append are O(1)), and the latest call decides
    @Test
    public void testReRegistrationKeepsOneRegistrationLogEntryPerClass() throws Exception {
        final FieldSerializer<LogTargetLocal> localSerializer = new FieldSerializer<>(new Kryo(), LogTargetLocal.class);
        final FieldSerializer<LogTargetGlobal> globalSerializer = new FieldSerializer<>(new Kryo(), LogTargetGlobal.class);
        final KryoParser parser = new KryoParser();

        synchronized (ParserFactory._kryoRegistrationLock) {
            final int globalLogSize = ParserFactory._kryoRegistrationOrder.size()
                    + (ParserFactory._kryoRegistrationOrder.contains(LogTargetGlobal.class) ? 0 : 1);

            try {
                for (int i = 0; i < 200; i++) {
                    switch (i % 4) {
                        case 0 -> {
                            parser.register(LogTargetLocal.class);
                            ParserFactory.registerKryo(LogTargetGlobal.class);
                        }
                        case 1 -> {
                            parser.register(LogTargetLocal.class, 1_147_001);
                            ParserFactory.registerKryo(LogTargetGlobal.class, 1_147_002);
                        }
                        case 2 -> {
                            parser.register(LogTargetLocal.class, localSerializer);
                            ParserFactory.registerKryo(LogTargetGlobal.class, globalSerializer);
                        }
                        default -> {
                            parser.register(LogTargetLocal.class, localSerializer, 1_147_003);
                            ParserFactory.registerKryo(LogTargetGlobal.class, globalSerializer, 1_147_004);
                        }
                    }
                }

                assertEquals(Set.of(LogTargetLocal.class), registrationOrderOf(parser));
                assertEquals(globalLogSize, ParserFactory._kryoRegistrationOrder.size());
                assertTrue(ParserFactory._kryoRegistrationOrder.contains(LogTargetGlobal.class));
                assertTrue(ParserFactory._kryoClassSerializerIdMap.containsKey(LogTargetGlobal.class));
                assertTrue(!ParserFactory._kryoClassSet.contains(LogTargetGlobal.class) && !ParserFactory._kryoClassIdMap.containsKey(LogTargetGlobal.class)
                        && !ParserFactory._kryoClassSerializerMap.containsKey(LogTargetGlobal.class));

                final Kryo kryo = parser.createKryo();

                try {
                    assertEquals(1_147_003, kryo.getRegistration(LogTargetLocal.class).getId());
                    assertSame(localSerializer, kryo.getRegistration(LogTargetLocal.class).getSerializer());
                    assertEquals(1_147_004, kryo.getRegistration(LogTargetGlobal.class).getId());
                    assertSame(globalSerializer, kryo.getRegistration(LogTargetGlobal.class).getSerializer());
                } finally {
                    parser.recycle(kryo);
                }
            } finally {
                ParserFactory._kryoClassSet.remove(LogTargetGlobal.class);
                ParserFactory._kryoClassIdMap.remove(LogTargetGlobal.class);
                ParserFactory._kryoClassSerializerMap.remove(LogTargetGlobal.class);
                ParserFactory._kryoClassSerializerIdMap.remove(LogTargetGlobal.class);
                ParserFactory._kryoRegistrationOrder.remove(LogTargetGlobal.class);
                ParserFactory._kryoRegistrationVersion.incrementAndGet();
            }
        }
    }

    // registrations through all four overloads racing createKryo/encode/decode on the same parser: no thread fails, the
    // registration log and the four collections end in sync, and a Kryo created afterwards pins every explicit ID and
    // gives the other classes ascending IDs, class-only before serializer-only, each in the order of the latest calls
    @Test
    public void testConcurrentRegistrationKeepsRegistrationLogInSyncWithRegistrations() throws Exception {
        final Class<?>[] types = { Race00.class, Race01.class, Race02.class, Race03.class, Race04.class, Race05.class, Race06.class, Race07.class, Race08.class,
                Race09.class, Race10.class, Race11.class };
        final Kryo serializerKryo = new Kryo();
        final Serializer<?>[] serializers = new Serializer<?>[types.length];

        for (int i = 0; i < types.length; i++) {
            serializers[i] = new FieldSerializer<>(serializerKryo, types[i]);
        }

        final KryoParser parser = new KryoParser();
        final AtomicBoolean registering = new AtomicBoolean(true);
        final AtomicReference<Throwable> failure = new AtomicReference<>();
        final CountDownLatch start = new CountDownLatch(1);
        final List<Thread> registrars = new ArrayList<>();
        final List<Thread> users = new ArrayList<>();

        for (int t = 0; t < 2; t++) {
            final Random random = new Random(t);
            registrars.add(new Thread(() -> {
                try {
                    start.await();

                    for (int i = 0; i < 2_000; i++) {
                        final int index = random.nextInt(types.length);
                        final int id = 1_147_100 + random.nextInt(40);

                        try {
                            switch (random.nextInt(4)) {
                                case 0 -> parser.register(types[index]);
                                case 1 -> parser.register(types[index], id);
                                case 2 -> parser.register(types[index], serializers[index]);
                                default -> parser.register(types[index], serializers[index], id);
                            }
                        } catch (final IllegalArgumentException e) {
                            // the explicit ID is held by another class
                        }
                    }
                } catch (final Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }));
        }

        for (int t = 0; t < 2; t++) {
            users.add(new Thread(() -> {
                try {
                    start.await();

                    while (registering.get()) {
                        parser.recycle(parser.createKryo());

                        try {
                            parser.decode(parser.encode(new Race05()));
                        } catch (final KryoException e) {
                            // encode and decode can use two Kryo instances created on either side of a registration
                        }
                    }
                } catch (final Throwable e) {
                    failure.compareAndSet(null, e);
                }
            }));
        }

        registrars.forEach(Thread::start);
        users.forEach(Thread::start);
        start.countDown();

        for (final Thread thread : registrars) {
            thread.join();
        }

        registering.set(false);

        for (final Thread thread : users) {
            thread.join();
        }

        assertNull(failure.get());

        @SuppressWarnings("unchecked")
        final Map<Class<?>, Integer> idMap = (Map<Class<?>, Integer>) field(parser, "kryoClassIdMap");
        @SuppressWarnings("unchecked")
        final Map<Class<?>, Tuple2<Serializer<?>, Integer>> serializerIdMap = (Map<Class<?>, Tuple2<Serializer<?>, Integer>>) field(parser,
                "kryoClassSerializerIdMap");
        final List<Class<?>> order;

        synchronized (field(parser, "kryoPool")) {
            final Set<Class<?>> keys = new HashSet<>();
            int entries = 0;

            for (final Object collection : List.of(field(parser, "kryoClassSet"), idMap, field(parser, "kryoClassSerializerMap"), serializerIdMap)) {
                final Set<?> classes = collection instanceof Map<?, ?> map ? map.keySet() : (Set<?>) collection;
                entries += classes.size();

                for (final Object type : classes) {
                    keys.add((Class<?>) type);
                }
            }

            assertEquals(keys.size(), entries, "every class is in exactly one collection");
            assertEquals(keys, registrationOrderOf(parser));
            order = new ArrayList<>(registrationOrderOf(parser));
        }

        @SuppressWarnings("unchecked")
        final Set<Class<?>> classOnly = (Set<Class<?>>) field(parser, "kryoClassSet");
        final Kryo kryo = parser.createKryo();

        try {
            int previousAutomaticId = -1;

            for (final boolean classOnlyKind : new boolean[] { true, false }) {
                for (final Class<?> type : order) {
                    final int id = kryo.getRegistration(type).getId();
                    final Integer explicitId = idMap.containsKey(type) ? idMap.get(type)
                            : serializerIdMap.containsKey(type) ? serializerIdMap.get(type)._2 : null;

                    if (explicitId != null) {
                        assertEquals(explicitId.intValue(), id);
                    } else if (classOnly.contains(type) == classOnlyKind) {
                        assertTrue(id > previousAutomaticId, "automatic IDs ascend kind by kind in call order");
                        previousAutomaticId = id;
                    }
                }
            }
        } finally {
            parser.recycle(kryo);
        }

        for (final Class<?> type : types) {
            assertSame(type, parser.decode(parser.encode(type.getDeclaredConstructor().newInstance())).getClass());
        }
    }
}
