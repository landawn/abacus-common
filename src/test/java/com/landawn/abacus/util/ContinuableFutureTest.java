/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;

public class ContinuableFutureTest extends AbstractTest {

    //    @Test
    //    public void test_toCompletableFuture() throws Exception {
    //        N.asyncExecute(() -> "first call").thenRun(Fn.println()).get();
    //        N.asyncExecute(() -> "first call").toCompletableFuture().thenAcceptAsync(Fn.println()).get();
    //    }

    @Test
    public void test_02() throws Exception {

        N.println(System.currentTimeMillis());
        N.asyncExecute(
                (Throwables.Runnable<RuntimeException>) () -> N.println("1st run at: " + Thread.currentThread().getName() + ", " + System.currentTimeMillis()))
                .thenDelay(1000, TimeUnit.MILLISECONDS)
                .thenCall((Callable<String>) () -> N.println("2nd apply at: " + Thread.currentThread().getName() + ", " + System.currentTimeMillis()));

        N.sleep(1000);
    }

    @Test
    public void test_ObservableFuture() {
        AsyncExecutor asyncExecutor = new AsyncExecutor();

        asyncExecutor.execute((Throwables.Runnable<RuntimeException>) () -> {
            N.println(System.currentTimeMillis());
            N.sleep(100);
            N.println(System.currentTimeMillis());
            N.println("abc");
        }).thenRun((BiConsumer<Void, Exception>) (value, e) -> N.println("e: " + e + ", result: " + value));

        N.println(System.currentTimeMillis());

        N.sleep(1000);
    }

    @Test
    public void test_ObservableFuture_02() {
        AsyncExecutor asyncExecutor = new AsyncExecutor();

        asyncExecutor.execute((Throwables.Runnable<RuntimeException>) () -> {
            N.println(System.currentTimeMillis());
            N.sleep(100);
            N.println(System.currentTimeMillis());
            N.println("abc");
            throw new RuntimeException();
        }).thenRun((BiConsumer<Void, Exception>) (value, e) -> N.println("e: " + e + ", result: " + value));

        N.println(System.currentTimeMillis());

        N.sleep(1000);
    }

    @Test
    public void test_ObservableFuture_03() {
        AsyncExecutor asyncExecutor = new AsyncExecutor(8, 16, 300, TimeUnit.SECONDS);

        for (int i = 0; i < 100; i++) {
            asyncExecutor.execute((Throwables.Runnable<RuntimeException>) () -> {
                N.println(System.currentTimeMillis());
                N.sleep(100);
                N.println(System.currentTimeMillis());
                N.println(Thread.currentThread());
                throw new RuntimeException();
            }).thenRun((BiConsumer<Void, Exception>) (value, e) -> N.println("e: " + e + ", result: " + value));
        }

        N.println(System.currentTimeMillis());

        N.sleep(1000);
    }

    @Test
    public void test_callback_execute() {
        AsyncExecutor asyncExecutor = new AsyncExecutor(8, 16, 300, TimeUnit.SECONDS);

        asyncExecutor.execute((Callable<String>) () -> {
            throw new RuntimeException();
        }).thenCall((BiFunction<String, Exception, String>) (result, e) -> {
            if (e != null) {
            }

            N.println("123: ");

            return "abc";
        }).thenRun((BiConsumer<String, Exception>) (value, e) -> N.println("e: " + e + ", result: " + value));

        N.println("#################");

        N.sleep(3000);
    }
}
