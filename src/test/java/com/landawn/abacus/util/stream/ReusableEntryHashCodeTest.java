package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ReusableEntryHashCodeTest extends TestBase {

    @Test
    public void testHashCodeDoesNotResetFlag() throws Exception {
        Class<?> reusableEntryClass = Class.forName("com.landawn.abacus.util.stream.EntryStream$ReusableEntry");
        Object entry = reusableEntryClass.getDeclaredConstructor().newInstance();

        var setMethod = reusableEntryClass.getMethod("set", Object.class, Object.class);
        setMethod.invoke(entry, "key1", "value1");

        // Call hashCode
        var hashCodeMethod = reusableEntryClass.getMethod("hashCode");
        int hash = (int) hashCodeMethod.invoke(entry);

        // Now try to set again - if flag wasn't reset, this will throw
        final InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
            setMethod.invoke(entry, "key2", "value2");
        }, "set() after hashCode() should throw IllegalStateException if flag wasn't reset by hashCode()");

        assertTrue(exception.getCause() instanceof IllegalStateException);
    }
}
