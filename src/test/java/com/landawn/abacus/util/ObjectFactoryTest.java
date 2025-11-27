package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class ObjectFactoryTest extends AbstractTest {

    @Test
    public void test_charBuffer() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        N.println(maxMemory / 1024 / 1024);

        List<Object> list = new ArrayList<>();
        for (int i = 0; i < 256; i++) {
            list.add(Objectory.createCharArrayBuffer());

            if (i % 100 == 0) {
                long freeMemory = Runtime.getRuntime().freeMemory();
                N.println(i + " : " + freeMemory * 1.0d / maxMemory + " : " + freeMemory);
            }
        }

        list.clear();

        for (int i = 0; i < 512; i++) {
            list.add(Objectory.createByteArrayBuffer());

            if (i % 100 == 0) {
                long freeMemory = Runtime.getRuntime().freeMemory();
                N.println(i + " : " + freeMemory * 1.0d / maxMemory + " : " + freeMemory);
            }
        }
    }

    @Test
    public void test_create() {
        Map<String, Object> m = Objectory.createMap();
        Objectory.recycle(m);

        m = Objectory.createLinkedHashMap();
        Objectory.recycle(m);
    }
}
