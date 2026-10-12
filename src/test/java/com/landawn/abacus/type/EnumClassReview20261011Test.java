package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class EnumClassReview20261011Test extends TestBase {

    public enum WithNestedTypes {
        FIRST {
            @Override
            public String toString() {
                return "first body";
            }
        },
        SECOND;

        public static final class Helper {
        }

        public interface NestedInterface {
        }
    }

    @Test
    void rejectsAnOrdinaryClassNestedInAnEnum() {
        assertThrows(IllegalArgumentException.class, () -> new EnumType<>(WithNestedTypes.Helper.class.getName()));
    }

    @Test
    void rejectsAnInterfaceNestedInAnEnum() {
        assertThrows(IllegalArgumentException.class, () -> new EnumType<>(WithNestedTypes.NestedInterface.class.getName()));
    }

    @Test
    void constantSpecificSubclassStillResolvesToItsEnum() {
        final EnumType<WithNestedTypes> type = new EnumType<>(WithNestedTypes.FIRST.getClass().getName());

        assertEquals(WithNestedTypes.class, type.javaType());
        assertEquals("FIRST", type.stringOf(WithNestedTypes.FIRST));
        assertEquals(WithNestedTypes.SECOND, type.valueOf("SECOND"));
    }
}
