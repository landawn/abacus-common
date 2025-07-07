package com.landawn.abacus;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;

public final class GeneratedTestUtil {
    private GeneratedTestUtil() {
        // Prevent instantiation
    }

    public static void unmap(MappedByteBuffer buffer) {
        if (buffer == null) {
            return;
        }

        try {
            buffer.force(); // flush changes to disk

            Method cleanerMethod = buffer.getClass().getMethod("cleaner");
            cleanerMethod.setAccessible(true);
            Object cleaner = cleanerMethod.invoke(buffer);

            Method cleanMethod = cleaner.getClass().getMethod("clean");
            cleanMethod.setAccessible(true);
            cleanMethod.invoke(cleaner);
        } catch (Exception e) {
            throw new RuntimeException("Failed to unmap the buffer", e);
        }
    }
}
