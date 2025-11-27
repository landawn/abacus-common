package com.landawn.abacus.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class ObserverTest extends AbstractTest {

    @Test
    public void test_0() {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(100);
        Observer.of(queue).observe(Fn.println(), Exception::printStackTrace, () -> N.println("completed"));

        queue.add("ab");
        queue.add("cc");
        queue.add("dd");

        Observer.timer(10).observe(Fn.println(), Exception::printStackTrace, () -> N.println("completed"));

        Observer.interval(100).observe(Fn.println(), Exception::printStackTrace, () -> N.println("completed"));

        N.sleep(3000);

    }

}
