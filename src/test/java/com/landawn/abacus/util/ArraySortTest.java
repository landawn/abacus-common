package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class ArraySortTest extends AbstractTest {

    @Test
    public void test_00() {
        assertDoesNotThrow(() -> {
            System.out.println("test nothing");
        });
    }

}
