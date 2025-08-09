package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

public class ExceptionUtilTest {

    @Test
    public void test_hasCause() {
        try {
            try {
                throw new IOException("e1");
            } catch (Exception e) {
                try {
                    throw new SQLException(e);
                } catch (Exception e2) {
                    try {
                        throw new ExecutionException(e2);
                    } catch (Exception e3) {
                        throw new RuntimeException(e3);
                    }
                }
            }
        } catch (Exception e) {
            assertTrue(ExceptionUtil.hasCause(e, SQLException.class));
            assertTrue(ExceptionUtil.hasCause(e, IOException.class));
        }
    }

}
