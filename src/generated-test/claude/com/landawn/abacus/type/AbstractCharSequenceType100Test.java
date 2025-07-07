package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractCharSequenceType100Test extends TestBase {
    private Type<String> type;

    @BeforeEach
    public void setUp() {
        type = createType(String.class);
    }

    @Test
    public void testIsCharSequence() {
        assertTrue(type.isCharSequence());
    }

    @Test
    public void testConstructor() {
        Type<String> stringType = createType(String.class);

        assertNotNull(stringType);
        assertTrue(stringType.isCharSequence());
    }

    @Test
    public void testIsCharSequence_DifferentImplementations() {
        // Test with StringBuilder type
        Type<StringBuilder> stringBuilderType = createType(StringBuilder.class);
        assertTrue(stringBuilderType.isCharSequence());

        // Test with StringBuffer type
        Type<StringBuffer> stringBufferType = createType(StringBuffer.class);

        assertTrue(stringBufferType.isCharSequence());
    }

    @Test
    public void testIsCharSequence_CustomCharSequence() {
        // Custom CharSequence implementation
        class CustomCharSequence implements CharSequence {
            private final String value;

            CustomCharSequence(String value) {
                this.value = value;
            }

            @Override
            public int length() {
                return value.length();
            }

            @Override
            public char charAt(int index) {
                return value.charAt(index);
            }

            @Override
            public CharSequence subSequence(int start, int end) {
                return value.subSequence(start, end);
            }

            @Override
            public String toString() {
                return value;
            }
        }

    }
}
