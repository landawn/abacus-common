package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class OperationTypeTest extends AbstractTest {

    @Test
    public void test_valueOf() {
        assertEquals(OperationType.QUERY, OperationType.valueOf(1));
        assertEquals(OperationType.ADD, OperationType.valueOf(2));
        assertEquals(OperationType.UPDATE, OperationType.valueOf(4));
        assertEquals(OperationType.DELETE, OperationType.valueOf(8));

        assertEquals(8, OperationType.DELETE.intValue());

        try {
            OperationType.valueOf(3);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {

        }
    }
}
