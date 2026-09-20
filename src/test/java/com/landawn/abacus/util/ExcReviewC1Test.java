package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

/**
 * Pins the exceptions now declared in the {@code throws} clauses of the lazy-supplier overloads
 * (throws-clause-only change; behaviour is unchanged and must be GREEN on the baseline too).
 */
public class ExcReviewC1Test extends com.landawn.abacus.TestBase {

    @Test
    public void checkArgument_supplier_nullSupplierIsOnlyDereferencedOnFailure() {
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, (Supplier<String>) null));
        assertThrows(NullPointerException.class, () -> CommonUtil.checkArgument(false, (Supplier<String>) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, () -> "msg"));
    }

    @Test
    public void checkState_supplier_nullSupplierIsOnlyDereferencedOnFailure() {
        assertDoesNotThrow(() -> CommonUtil.checkState(true, (Supplier<String>) null));
        assertThrows(NullPointerException.class, () -> CommonUtil.checkState(false, (Supplier<String>) null));
        assertThrows(IllegalStateException.class, () -> CommonUtil.checkState(false, () -> "msg"));
    }

    @Test
    public void equals_ranged_declaredExceptionsMatchTheJavadoc() {
        final int[] a = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a, 0, a, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a, -1, a, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a, 0, a, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a, 0, (int[]) null, 0, 1));
        assertDoesNotThrow(() -> CommonUtil.equals((int[]) null, 0, (int[]) null, 0, 0));
    }
}
