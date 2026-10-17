package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.ParserUtil;

@Tag("unit")
class BeansClassificationRegistrationTest {
    @Test
    void excludingLastPropertyCannotRepublishOldClassification() throws Exception {
        try (RegistrationState ignored = new RegistrationState(ExcludedProperty.class, "registeredNonPropGetSetMethodPool")) {
            assertEquals(List.of("value"), Beans.getPropNameList(ExcludedProperty.class));
            raceClassification(ExcludedProperty.class, () -> Beans.registerNonPropertyAccessor(ExcludedProperty.class, "value"));
            assertTrue(Beans.getPropNameList(ExcludedProperty.class).isEmpty());
            assertFalse(Beans.isBeanClass(ExcludedProperty.class), "A completed registration must not leave the old true answer cached");
            assertFalse(Beans.isBeanClass(ExcludedProperty.class));
        }
    }

    @Test
    void registeringXmlGetterCannotRepublishOldClassification() throws Exception {
        try (RegistrationState ignored = new RegistrationState(XmlGetter.class, "registeredXmlBindingClassList")) {
            assertTrue(Beans.getPropNameList(XmlGetter.class).isEmpty());
            raceClassification(XmlGetter.class, () -> Beans.registerXmlBindingClass(XmlGetter.class));
            assertEquals(List.of("values"), Beans.getPropNameList(XmlGetter.class));
            assertTrue(Beans.isBeanClass(XmlGetter.class), "A completed registration must not leave the old false answer cached");
            assertTrue(Beans.isBeanClass(XmlGetter.class));
        }
    }

    private static void raceClassification(final Class<?> type, final Runnable registration) throws Exception {
        final List<String> properties = Beans.getPropNameList(type);
        final CountDownLatch snapshotRead = new CountDownLatch(1);
        final CountDownLatch resume = new CountDownLatch(1);
        final AtomicBoolean firstSize = new AtomicBoolean(true);
        final List<String> pausedSnapshot = new AbstractList<>() {
            @Override
            public String get(final int index) {
                return properties.get(index);
            }

            @Override
            public int size() {
                if (firstSize.compareAndSet(true, false)) {
                    snapshotRead.countDown();
                    try {
                        if (!resume.await(10, TimeUnit.SECONDS)) {
                            throw new AssertionError("Registration did not release classifier");
                        }
                    } catch (final InterruptedException error) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(error);
                    }
                }
                return properties.size();
            }
        };
        // Keep the real property contents, pausing only after isBeanClass has obtained its immutable snapshot.
        cachePut(readField(Beans.class, "beanDeclaredPropNameListPool"), type, ImmutableList.wrap(pausedSnapshot));
        cachePut(readField(Beans.class, "beanClassPool"), type, null);
        final ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            final Future<Boolean> inFlight = worker.submit(() -> Beans.isBeanClass(type));
            assertTrue(snapshotRead.await(10, TimeUnit.SECONDS), "Classifier did not reach the old property snapshot");
            registration.run();
            resume.countDown();
            // Either answer is valid for this overlapping call; only later calls must reflect registration.
            inFlight.get(10, TimeUnit.SECONDS);
        } finally {
            resume.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static Object readField(final Class<?> owner, final String name) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static Object cacheGet(final Object cache, final Class<?> type) throws Exception {
        final Method get = cache.getClass().getDeclaredMethod("get", Class.class);
        get.setAccessible(true);
        return get.invoke(cache, type);
    }

    private static void cachePut(final Object cache, final Class<?> type, final Object value) throws Exception {
        final Method put = cache.getClass().getDeclaredMethod("put", Class.class, Object.class);
        put.setAccessible(true);
        put.invoke(cache, type, value);
    }

    private static void restoreEntry(final Map<Object, Object> map, final Object key, final Object value) {
        if (value == null) {
            map.remove(key);
        } else {
            map.put(key, value);
        }
    }

    private static final class RegistrationState implements AutoCloseable {
        private final Class<?> type;
        private final Object metadataLock;
        private final Map<Object, Object> registry;
        private final Object originalRegistration;
        private final Map<Object, Object> cacheEntries = new LinkedHashMap<>();
        private final Map<Object, Object> introspected;
        private final Object originalIntrospection;
        private final Map<Object, Object> parserCache;
        private final Object originalBeanInfo;

        @SuppressWarnings("unchecked")
        RegistrationState(final Class<?> type, final String registryName) throws Exception {
            this.type = type;
            metadataLock = readField(Beans.class, "METADATA_LOCK");
            registry = (Map<Object, Object>) readField(Beans.class, registryName);
            introspected = (Map<Object, Object>) readField(Beans.class, "introspectedClasses");
            synchronized (metadataLock) {
                final Object value = registry.get(type);
                originalRegistration = value instanceof Set<?> names ? new HashSet<>(names) : value;
                originalIntrospection = introspected.get(type);
                for (final Field field : Beans.class.getDeclaredFields()) {
                    if (field.getType().getSimpleName().equals("ClassCache")) {
                        field.setAccessible(true);
                        final Object cache = field.get(null);
                        cacheEntries.put(cache, cacheGet(cache, type));
                    }
                }
            }
            parserCache = (Map<Object, Object>) readField(ParserUtil.class, "beanInfoPool");
            synchronized (parserCache) {
                originalBeanInfo = parserCache.get(type);
            }
        }

        @Override
        public void close() throws Exception {
            // The dedicated final fixture has no affected subtypes. Restore its registration, every Beans
            // cache entry, subtype-index entry, and parser entry after the worker has terminated.
            synchronized (metadataLock) {
                restoreEntry(registry, type, originalRegistration);
                // Keep the global epoch monotonic so unrelated concurrent scans cannot miss this restoration.
                ((AtomicLong) readField(Beans.class, "beanMetadataEpoch")).incrementAndGet();
                for (final Map.Entry<Object, Object> entry : cacheEntries.entrySet()) {
                    cachePut(entry.getKey(), type, entry.getValue());
                }
                restoreEntry(introspected, type, originalIntrospection);
            }
            synchronized (parserCache) {
                restoreEntry(parserCache, type, originalBeanInfo);
            }
        }
    }

    public static final class ExcludedProperty {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static final class XmlGetter {
        private final List<String> values = new ArrayList<>();

        public List<String> getValues() {
            return values;
        }
    }
}
