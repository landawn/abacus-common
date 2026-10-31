package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.annotation.AccessFieldByMethod;

@Tag("unit")
@SuppressWarnings("deprecation")
class ParserMetadataPerformance20261030Test {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void recurringConversionsDoNotReturnToExceptionDrivenAssignment(final boolean asm) {
        final ParserUtil.PropInfo property = property(Value.class, asm);
        final Value bean = new Value();
        learnConversion(property, bean);
        final int learnedFailures = property.failureCountForSetProp;
        assertTrue(learnedFailures > 100);

        for (int i = 0; i < 1000; i++) {
            property.setPropValue(bean, "142");
            assertEquals(142, bean.value);
            assertEquals(learnedFailures, property.failureCountForSetProp,
                    "Converted success must not re-enable the raw assignment that throws for the same input type");
        }

        // A change to compatible boxed primitive input must still return to the ordinary assignment path.
        for (int i = 0; i < learnedFailures; i++) {
            property.setPropValue(bean, Integer.valueOf(43));
        }
        assertEquals(43, bean.value);
        assertEquals(0, property.failureCountForSetProp);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void learnedConversionPreservesPrimitiveWideningAndGeneratedWrapperCasts(final boolean asm) {
        final ParserUtil.PropInfo property = property(Value.class, asm);
        final Value bean = new Value();
        learnConversion(property, bean);
        final int learnedFailures = property.failureCountForSetProp;
        property.setPropValue(bean, Short.valueOf((short) 7));
        assertEquals(7, bean.value);
        assertEquals(asm ? learnedFailures : learnedFailures - 1, property.failureCountForSetProp);

        if (!asm) {
            // Reflection widens Character numerically; converting its text would change this assignment.
            property.setPropValue(bean, Character.valueOf('A'));
            assertEquals(65, bean.value);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectedSetterStillRunsOnceAndPropagatesItsOriginalFailure(final boolean asm) {
        final ParserUtil.PropInfo property = property(CheckedValue.class, asm);
        final CheckedValue bean = new CheckedValue();
        learnConversion(property, bean);
        bean.writes = 0;
        assertSame(bean.failure, assertThrows(IllegalArgumentException.class, () -> property.setPropValue(bean, "13")));
        assertEquals(1, bean.writes);
        assertEquals(142, bean.value);

        bean.writes = 0;
        assertSame(bean.failure, assertThrows(IllegalArgumentException.class, () -> property.setPropValue(bean, Integer.valueOf(13))));
        assertEquals(1, bean.writes);
    }

    @Test
    void configuredJsonConversionsKeepTheirResultsAfterWarmup() {
        final JsonParser parser = ParserFactory.createJsonParser();
        final JsonDeserConfig config = JsonDeserConfig.create().setValueType("value", String.class);
        final ParserUtil.PropInfo property = ParserUtil.getBeanInfo(JsonValue.class).getPropInfo("value");
        for (int i = 0; i < 200; i++) {
            assertEquals(142, parser.deserialize("{\"value\":\"142\"}", config, JsonValue.class).value);
        }
        final int learnedFailures = property.failureCountForSetProp;
        for (int i = 0; i < 1000; i++) {
            assertEquals(142, parser.deserialize("{\"value\":\"142\"}", config, JsonValue.class).value);
            assertEquals(learnedFailures, property.failureCountForSetProp);
        }
    }

    private static ParserUtil.PropInfo property(final Class<?> type, final boolean asm) {
        assumeTrue(!asm || ASMUtil.isASMAvailable());
        final ParserUtil.PropInfo property = ParserUtil.getBeanInfo(type, asm).getPropInfo("value");
        assertEquals(asm, property instanceof ParserUtil.ASMPropInfo);
        return property;
    }

    private static void learnConversion(final ParserUtil.PropInfo property, final Object bean) {
        for (int i = 0; i < 200; i++) {
            property.setPropValue(bean, "142");
        }
    }

    public static class Value {
        public int value;
    }

    public static class JsonValue {
        public int value;
    }

    @AccessFieldByMethod
    public static class CheckedValue {
        private int value;
        private int writes;
        private final IllegalArgumentException failure = new IllegalArgumentException("rejected");

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            writes++;
            if (value == 13) {
                throw failure;
            }
            this.value = value;
        }
    }
}
