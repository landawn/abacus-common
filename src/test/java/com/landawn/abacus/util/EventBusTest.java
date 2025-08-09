/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.eventbus.EventBus;
import com.landawn.abacus.eventbus.Subscribe;
import com.landawn.abacus.eventbus.Subscriber;

public class EventBusTest extends AbstractTest {

    @Test
    public void test_01() {
        final Object strSubscriber_1 = new Subscriber<String>() {
            @Override
            public void on(String event) {
                System.out.println("Subscriber: strSubscriber_1, event: " + event);
            }
        };

        final Object anySubscriber_2 = new Object() {
            @Subscribe(threadMode = ThreadMode.DEFAULT, interval = 1000)
            public void anyMethod(Object event) {
                System.out.println("Subscriber: anySubscriber_2, event: " + event);
            }
        };

        final Object anySubscriber_3 = new Object() {
            @Subscribe(threadMode = ThreadMode.DEFAULT, sticky = true, deduplicate = true)
            public void anyMethod(Object event) {
                System.out.println("Subscriber: anySubscriber_3, event: " + event);
            }
        };

        final EventBus eventBus = EventBus.getDefault();

        eventBus.register(strSubscriber_1);
        eventBus.register(strSubscriber_1);
        eventBus.register(anySubscriber_2, "eventId_2");

        eventBus.post("abc");
        eventBus.postSticky("sticky");
        eventBus.post("eventId_2", "abc");

        eventBus.post(123);
        eventBus.post("eventId_2", 123);

        eventBus.register(anySubscriber_3);
        eventBus.post("sticky1");
        eventBus.post("sticky");
        eventBus.post("sticky");
    }
}
