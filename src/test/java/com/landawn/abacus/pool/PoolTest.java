package com.landawn.abacus.pool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Seid;
import com.landawn.abacus.util.Strings;

@Tag("old-test")
public class PoolTest extends AbstractTest {

    @Test
    public void testMaxWaitTime() {
        final int capacity = 30;
        final ObjectPool<PoolableWrapper> pool = PoolFactory.createObjectPool(capacity, 30);

        for (int i = 0; i < capacity; i++) {
            pool.add(new PoolableWrapper<>(i, 600, 600));
        }

        final long startTime = System.currentTimeMillis();
        final AtomicInteger counter = new AtomicInteger();
        final int threadNum = 40;

        for (int i = 0; i < threadNum; i++) {
            final Thread th = new Thread() {
                @Override
                public void run() {
                    try {
                        final PoolableWrapper result = pool.take(2, TimeUnit.SECONDS);

                        if (result == null) {
                            counter.incrementAndGet();
                        } else {
                            N.println(result.value());
                        }
                    } catch (final InterruptedException e) {
                        counter.incrementAndGet();
                        e.printStackTrace();
                    }
                }
            };

            th.start();
        }

        while (counter.get() < (threadNum - capacity)) {
            try {
                Thread.sleep(10);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }

        N.println(System.currentTimeMillis() - startTime);
    }

    @Test
    public void test_GenericObjectPool() throws InterruptedException {
        ObjectPool<PoolableWrapper<String>> objectPool = PoolFactory.createObjectPool(3000, 1000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        objectPool.add(Poolable.wrap("123"));

        assertTrue(objectPool.contains(Poolable.wrap("123")));

        for (int i = 0; i < 3000; i++) {
            objectPool.add(Poolable.wrap(N.stringOf(i)));
        }

        assertFalse(objectPool.add(Poolable.wrap("123")));

        assertFalse(objectPool.add(Poolable.wrap("123"), 100, TimeUnit.MILLISECONDS));

        objectPool = PoolFactory.createObjectPool(1000, 1000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);

        objectPool.add(Poolable.wrap("123"));

        assertTrue(objectPool.contains(Poolable.wrap("123")));

        for (int i = 0; i < 30000; i++) {
            objectPool.add(Poolable.wrap(N.stringOf(i)));
        }

        assertTrue(objectPool.add(Poolable.wrap("123")));

        assertTrue(objectPool.add(Poolable.wrap("123"), 100, TimeUnit.MILLISECONDS));

        final PoolableWrapper<String> value = Poolable.wrap("123", 10, 10);
        N.sleep(100);

        assertFalse(objectPool.add(value));

        try {
            objectPool.add(null);
        } catch (final IllegalArgumentException e) {

        }

        try {

        } catch (final IllegalArgumentException e) {

        }
    }

    @Test
    public void test_GenericKeyedObjectPool() {

        try {
            PoolFactory.createKeyedObjectPool(-1, 100);
        } catch (final IllegalArgumentException e) {

        }

        try {
            PoolFactory.createKeyedObjectPool(1000, -10);
        } catch (final IllegalArgumentException e) {

        }

        KeyedObjectPool<String, PoolableWrapper<String>> keyedObjectPool = PoolFactory.createKeyedObjectPool(1000, 100);

        keyedObjectPool.put("abc", Poolable.wrap("123"));

        try {
            keyedObjectPool.put(null, Poolable.wrap("123"));
        } catch (final IllegalArgumentException e) {

        }

        try {
            keyedObjectPool.put("abc", null);
        } catch (final IllegalArgumentException e) {

        }

        final PoolableWrapper<String> value = Poolable.wrap("123", 10, 10);
        N.sleep(100);

        assertFalse(keyedObjectPool.put("abc", value));

        assertTrue(keyedObjectPool.containsKey("abc"));

        N.println(keyedObjectPool.values());

        keyedObjectPool = PoolFactory.createKeyedObjectPool(1000, 1000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        for (int i = 0; i < 1000; i++) {
            keyedObjectPool.put(Strings.uuid(), Poolable.wrap(N.stringOf(i)));
        }

        assertFalse(keyedObjectPool.put(Strings.uuid(), Poolable.wrap("123")));

        keyedObjectPool = PoolFactory.createKeyedObjectPool(1000, 1000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);

        for (int i = 0; i < 1000; i++) {
            keyedObjectPool.put(Strings.uuid(), Poolable.wrap(N.stringOf(i)));
        }

        assertTrue(keyedObjectPool.put(Strings.uuid(), Poolable.wrap("123")));

        keyedObjectPool.close();
    }

    @Test
    public void testGenericKeyedObjectPool1() throws Exception {
        final KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000);
        pool.put("a", Poolable.wrap("a", 1000, 1000));
        N.println(pool.size());
        assertEquals(1, pool.size());
        Thread.sleep(3000);
        assertEquals(0, pool.size());
        pool.close();
    }

    @Test
    public void testGenericKeyedObjectPool2() throws Exception {
        final KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000);
        pool.put("a", Poolable.wrap("a", 1000, 1000));
        N.println(pool.size());
        assertEquals(1, pool.size());
        pool.clear();
        assertEquals(0, pool.size());

        pool.close();
    }

    @Test
    public void testGenericKeyedObjectPool3() throws Exception {
        final KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000);
        pool.put("a", Poolable.wrap("a", 1000, 1000));
        N.println(pool.size());
        assertEquals(1, pool.size());
        pool.close();

        try {
            pool.put("a", Poolable.wrap("a", 1000, 1000));
            fail("should threw RuntimeException");
        } catch (final RuntimeException e) {
        }
    }

    @Test
    public void test_vacation() throws Exception {
        KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.1f);

        for (int i = 0; i < 1000; i++) {
            pool.put(String.valueOf(i), Poolable.wrap(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createKeyedObjectPool(100, 2000, EvictionPolicy.ACCESS_COUNT, true, 0.2f);

        for (int i = 0; i < 1000; i++) {
            pool.put(String.valueOf(i), Poolable.wrap(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createKeyedObjectPool(100, 2000, EvictionPolicy.EXPIRATION_TIME, true, 0.3f);

        for (int i = 0; i < 1000; i++) {
            pool.put(String.valueOf(i), Poolable.wrap(String.valueOf(i)));
            N.sleep(1);
        }

        pool.get(String.valueOf(1));

        N.println(pool.size());
        N.println(pool);

        N.println(pool.stats());
    }

    @Test
    public void test_vacation_2() throws Exception {
        ObjectPool<PoolableWrapper<String>> pool = PoolFactory.createObjectPool(100, 2000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.1f);

        for (int i = 0; i < 1000; i++) {
            pool.add(Poolable.wrap(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createObjectPool(100, 2000, EvictionPolicy.ACCESS_COUNT, true, 0.2f);

        for (int i = 0; i < 1000; i++) {
            pool.add(Poolable.wrap(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createObjectPool(100, 2000, EvictionPolicy.EXPIRATION_TIME, true, 0.3f);

        for (int i = 0; i < 1000; i++) {
            pool.add(Poolable.wrap(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);
    }

    @Test
    public void test_Wrapper() {
        PoolableWrapper<Seid> wrapper = Poolable.wrap(Seid.of(AccountPNL.ID, 1));

        N.println(wrapper);

        final PoolableWrapper<Seid> wrapper2 = Poolable.wrap(Seid.of(AccountPNL.ID, 1));

        assertTrue(wrapper.equals(wrapper2));
        assertTrue(N.asSet(wrapper).contains(wrapper2));

        N.println(wrapper.value());

        wrapper = new PoolableWrapper<>(wrapper.value());
        N.println(wrapper.value());
    }

    @Test
    public void test_ActivityPrint() {
        ActivityPrint activityPrint = new ActivityPrint(100, 100);

        N.println(activityPrint);

        final ActivityPrint activityPrint2 = new ActivityPrint(100, 100);

        assertTrue(activityPrint.equals(activityPrint.clone()));
        assertTrue(N.asSet(activityPrint).contains(activityPrint.clone()));

        activityPrint2.setLiveTime(1);
        activityPrint2.setMaxIdleTime(100);

        assertFalse(activityPrint.equals(activityPrint2));
        assertFalse(activityPrint.equals(activityPrint2.clone()));

        try {
            activityPrint = new ActivityPrint(-1, 100);
            N.println(activityPrint);
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            activityPrint = new ActivityPrint(100, -1);
            N.println(activityPrint);
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            activityPrint2.setLiveTime(-1);
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        try {
            activityPrint2.setMaxIdleTime(-1);
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }
    }
}
