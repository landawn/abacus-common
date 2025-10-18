/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.pool.ActivityPrint;
import com.landawn.abacus.pool.EvictionPolicy;
import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.ObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.pool.PoolableWrapper;

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
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        N.println(System.currentTimeMillis() - startTime);
    }

    //
    //    public void testFIFO() {
    //        ObjectPool<Wrapper<String>> pool = PoolFactory.createObjectPool(3, 0);
    //
    //        pool.add(Wrapper.valueOf("a"));
    //        pool.add(Wrapper.valueOf("b"));
    //        pool.add(Wrapper.valueOf("c"));
    //
    //        String st = pool.take().get() + pool.take().get() + pool.take().get();
    //        N.println(st);
    //        assertEquals("abc", st);
    //    }

    @Test
    public void test_GenericObjectPool() throws InterruptedException {
        ObjectPool<PoolableWrapper<String>> objectPool = PoolFactory.createObjectPool(3000, 1000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        objectPool.add(PoolableWrapper.of("123"));

        assertTrue(objectPool.contains(PoolableWrapper.of("123")));

        for (int i = 0; i < 3000; i++) {
            objectPool.add(PoolableWrapper.of(CommonUtil.stringOf(i)));
        }

        assertFalse(objectPool.add(PoolableWrapper.of("123")));

        assertFalse(objectPool.add(PoolableWrapper.of("123"), 100, TimeUnit.MILLISECONDS));

        objectPool = PoolFactory.createObjectPool(1000, 1000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);

        objectPool.add(PoolableWrapper.of("123"));

        assertTrue(objectPool.contains(PoolableWrapper.of("123")));

        for (int i = 0; i < 30000; i++) {
            objectPool.add(PoolableWrapper.of(CommonUtil.stringOf(i)));
        }

        assertTrue(objectPool.add(PoolableWrapper.of("123")));

        assertTrue(objectPool.add(PoolableWrapper.of("123"), 100, TimeUnit.MILLISECONDS));

        final PoolableWrapper<String> value = PoolableWrapper.of("123", 10, 10);
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

        keyedObjectPool.put("abc", PoolableWrapper.of("123"));

        try {
            keyedObjectPool.put(null, PoolableWrapper.of("123"));
        } catch (final IllegalArgumentException e) {

        }

        try {
            keyedObjectPool.put("abc", null);
        } catch (final IllegalArgumentException e) {

        }

        final PoolableWrapper<String> value = PoolableWrapper.of("123", 10, 10);
        N.sleep(100);

        assertFalse(keyedObjectPool.put("abc", value));

        assertTrue(keyedObjectPool.containsKey("abc"));
        // assertTrue(keyedObjectPool.containsValue(PoolableWrapper.of("123")));

        N.println(keyedObjectPool.values());

        keyedObjectPool = PoolFactory.createKeyedObjectPool(1000, 1000, EvictionPolicy.LAST_ACCESS_TIME, false, 0.2f);

        for (int i = 0; i < 1000; i++) {
            keyedObjectPool.put(Strings.uuid(), PoolableWrapper.of(CommonUtil.stringOf(i)));
        }

        assertFalse(keyedObjectPool.put(Strings.uuid(), PoolableWrapper.of("123")));

        keyedObjectPool = PoolFactory.createKeyedObjectPool(1000, 1000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.2f);

        for (int i = 0; i < 1000; i++) {
            keyedObjectPool.put(Strings.uuid(), PoolableWrapper.of(CommonUtil.stringOf(i)));
        }

        assertTrue(keyedObjectPool.put(Strings.uuid(), PoolableWrapper.of("123")));

        keyedObjectPool.close();
    }

    //    public void testLIFO() {
    //        ObjectPool<Wrapper<String>> pool = new GenericObjectPool<Wrapper<String>>(3, 0, false, 0.2f,
    //                VacationScale.ACCESS_COUNT, new ArrayDeque());
    //
    //        pool.offer(Wrapper.valueOf("a"));
    //        pool.offer(Wrapper.valueOf("b"));
    //        pool.offer(Wrapper.valueOf("c"));
    //        N.println(pool.peek().get() + pool.peek().get() + pool.peek().get());
    //
    //        String st = pool.poll().get() + pool.poll().get() + pool.poll().get();
    //        N.println(st);
    //        assertEquals("cba", st);
    //    }
    @Test
    public void testGenericKeyedObjectPool1() throws Exception {
        final KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000);
        pool.put("a", PoolableWrapper.of("a", 1000, 1000));
        N.println(pool.size());
        assertEquals(1, pool.size());
        Thread.sleep(3000);
        assertEquals(0, pool.size());
        pool.close();
    }

    @Test
    public void testGenericKeyedObjectPool2() throws Exception {
        final KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000);
        pool.put("a", PoolableWrapper.of("a", 1000, 1000));
        N.println(pool.size());
        assertEquals(1, pool.size());
        pool.clear();
        assertEquals(0, pool.size());

        pool.close();
    }

    @Test
    public void testGenericKeyedObjectPool3() throws Exception {
        final KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000);
        pool.put("a", PoolableWrapper.of("a", 1000, 1000));
        N.println(pool.size());
        assertEquals(1, pool.size());
        pool.close();

        try {
            pool.put("a", PoolableWrapper.of("a", 1000, 1000));
            fail("should threw RuntimeException");
        } catch (final RuntimeException e) {
        }
    }

    @Test
    public void test_vacation() throws Exception {
        KeyedObjectPool<String, PoolableWrapper<String>> pool = PoolFactory.createKeyedObjectPool(100, 2000, EvictionPolicy.LAST_ACCESS_TIME, true, 0.1f);

        for (int i = 0; i < 1000; i++) {
            pool.put(String.valueOf(i), PoolableWrapper.of(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createKeyedObjectPool(100, 2000, EvictionPolicy.ACCESS_COUNT, true, 0.2f);

        for (int i = 0; i < 1000; i++) {
            pool.put(String.valueOf(i), PoolableWrapper.of(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createKeyedObjectPool(100, 2000, EvictionPolicy.EXPIRATION_TIME, true, 0.3f);

        for (int i = 0; i < 1000; i++) {
            pool.put(String.valueOf(i), PoolableWrapper.of(String.valueOf(i)));
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
            pool.add(PoolableWrapper.of(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createObjectPool(100, 2000, EvictionPolicy.ACCESS_COUNT, true, 0.2f);

        for (int i = 0; i < 1000; i++) {
            pool.add(PoolableWrapper.of(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);

        pool = PoolFactory.createObjectPool(100, 2000, EvictionPolicy.EXPIRATION_TIME, true, 0.3f);

        for (int i = 0; i < 1000; i++) {
            pool.add(PoolableWrapper.of(String.valueOf(i)));
            N.sleep(1);
        }

        N.println(pool.size());
        N.println(pool);
    }

    @Test
    public void test_Wrapper() {
        PoolableWrapper<Seid> wrapper = PoolableWrapper.of(Seid.of(AccountPNL.ID, 1));

        N.println(wrapper);

        final PoolableWrapper<Seid> wrapper2 = PoolableWrapper.of(Seid.of(AccountPNL.ID, 1));

        assertTrue(wrapper.equals(wrapper2));
        assertTrue(CommonUtil.asSet(wrapper).contains(wrapper2));

        N.println(wrapper.value());

        wrapper = new PoolableWrapper<>(wrapper.value());
        N.println(wrapper.value());
    }

    @Test
    public void test_ActivityPrint() {
        ActivityPrint activityPrint = new ActivityPrint(100, 100);

        N.println(activityPrint);

        final ActivityPrint activityPrint2 = new ActivityPrint(100, 100);

        // assertTrue(activityPrint.equals(activityPrint2));
        assertTrue(activityPrint.equals(activityPrint.clone()));
        assertTrue(CommonUtil.asSet(activityPrint).contains(activityPrint.clone()));

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
