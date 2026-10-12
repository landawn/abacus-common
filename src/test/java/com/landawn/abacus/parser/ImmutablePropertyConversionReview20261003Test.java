package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;

@Tag("unit")
@SuppressWarnings("deprecation")
public class ImmutablePropertyConversionReview20261003Test extends TestBase {
    public static class TextSource {
        public String value = "42";
    }

    public static class MutableTarget {
        public int value;
    }

    public record IntTarget(int value) {
    }

    public record IntegerTarget(Integer value) {
    }

    public record BooleanTarget(boolean value) {
    }

    public record ByteTarget(byte value) {
    }

    public record ShortTarget(short value) {
    }

    public record CharTarget(char value) {
    }

    public record LongTarget(long value) {
    }

    public record FloatTarget(float value) {
    }

    public record DoubleTarget(double value) {
    }

    public record ListTarget(List<Object> value) {
    }

    public record RawTarget(@JsonXmlField(isJsonRawValue = true) String value) {
    }

    public static final class ImmutableTarget {
        private final int value;

        public ImmutableTarget(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "all", "selected", "filtered", "renamed", "excluded" })
    void everyCopyFamilyConvertsConstructorArguments(final String mode) {
        final TextSource source = new TextSource();
        final IntTarget result = switch (mode) {
            case "selected" -> Beans.copyAs(source, List.of("value"), IntTarget.class);
            case "filtered" -> Beans.copyAs(source, (name, value) -> true, IntTarget.class);
            case "renamed" -> Beans.copyAs(source, List.of("value"), name -> "value", IntTarget.class);
            case "excluded" -> Beans.copyAs(source, false, Collections.emptySet(), IntTarget.class);
            default -> Beans.copyAs(source, IntTarget.class);
        };
        assertEquals(42, result.value());
    }

    @Test
    void mapToRecordConvertsTextLikeMutableBean() {
        final Map<String, Object> values = Map.of("value", "42");
        assertEquals(42, Beans.mapToBean(values, MutableTarget.class).value);
        assertEquals(42, Beans.mapToBean(values, IntTarget.class).value());
    }

    @Test
    void numericWrapperConversionIsAppliedToRecord() {
        assertEquals(42, Beans.mapToBean(Map.of("value", 42L), IntegerTarget.class).value());
    }

    @Test
    void immutableAllArgumentsClassReceivesConvertedValue() {
        assertEquals(42, Beans.copyAs(new TextSource(), ImmutableTarget.class).getValue());
    }

    @Test
    void configuredJsonValueTypeStillConvertsToConstructorProperty() {
        final JsonDeserConfig config = new JsonDeserConfig().setValueType("value", String.class);
        final IntTarget result = ParserFactory.createJsonParser().deserialize("{\"value\":\"42\"}", config, IntTarget.class);
        assertEquals(42, result.value());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void directMetadataConvertsTextAndNumericWrappers(final boolean asm) {
        final ParserUtil.BeanInfo primitive = info(IntTarget.class, asm);
        final Object primitiveResult = primitive.createBeanResult();
        primitive.getPropInfo("value").setPropValue(primitiveResult, "42");
        assertEquals(new IntTarget(42), primitive.finishBeanResult(primitiveResult));

        final ParserUtil.BeanInfo wrapper = info(IntegerTarget.class, asm);
        final Object wrapperResult = wrapper.createBeanResult();
        wrapper.setPropValue(wrapperResult, "value", 42L);
        assertEquals(new IntegerTarget(42), wrapper.finishBeanResult(wrapperResult));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void primitiveInvocationCompatibilityMatchesReflection(final boolean asm) throws Exception {
        final Class<?>[] targets = { BooleanTarget.class, ByteTarget.class, ShortTarget.class, CharTarget.class, IntTarget.class, LongTarget.class,
                FloatTarget.class, DoubleTarget.class };
        final Object[] values = { true, (byte) 12, (short) 12, 'A', 12, 12L, 12F, 12D };
        int compatible = 0;
        for (final Class<?> target : targets) {
            final Constructor<?> constructor = target.getDeclaredConstructors()[0];
            final ParserUtil.BeanInfo beanInfo = info(target, asm);
            for (final Object value : values) {
                final Object expected;
                try {
                    expected = constructor.newInstance(value);
                } catch (final IllegalArgumentException incompatible) {
                    continue;
                }
                final Object[] arguments = (Object[]) beanInfo.createBeanResult();
                beanInfo.getPropInfo("value").setPropValue(arguments, value);
                assertSame(value, arguments[0], target.getSimpleName() + " from " + value.getClass().getSimpleName());
                assertEquals(expected, beanInfo.finishBeanResult(arguments));
                compatible++;
            }
        }
        assertEquals(27, compatible);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullDefaultsAndCompatibleReferenceIdentityArePreserved(final boolean asm) {
        final ParserUtil.BeanInfo primitive = info(IntTarget.class, asm);
        final Object primitiveResult = primitive.createBeanResult();
        primitive.getPropInfo("value").setPropValue(primitiveResult, null);
        assertEquals(new IntTarget(0), primitive.finishBeanResult(primitiveResult));

        final ParserUtil.BeanInfo wrapper = info(IntegerTarget.class, asm);
        final Object wrapperResult = wrapper.createBeanResult();
        wrapper.getPropInfo("value").setPropValue(wrapperResult, null);
        final IntegerTarget nullValue = wrapper.finishBeanResult(wrapperResult);
        assertNull(nullValue.value());

        final List<Object> value = new ArrayList<>();
        value.add(new Object());
        final ParserUtil.BeanInfo list = info(ListTarget.class, asm);
        final Object listResult = list.createBeanResult();
        list.getPropInfo("value").setPropValue(listResult, value);
        final ListTarget result = list.finishBeanResult(listResult);
        assertSame(value, result.value());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rawJsonNormalizationStillPrecedesConstructorStorage(final boolean asm) {
        final ParserUtil.BeanInfo beanInfo = info(RawTarget.class, asm);
        final Object result = beanInfo.createBeanResult();
        final List<String> values = List.of("a", "quoted\"value");
        beanInfo.getPropInfo("value").setPropValue(result, values);
        final RawTarget raw = beanInfo.finishBeanResult(result);
        assertEquals(N.toJson(values), raw.value());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void unconvertibleValuesStillFail(final boolean asm) {
        final ParserUtil.BeanInfo beanInfo = info(IntTarget.class, asm);
        final Object result = beanInfo.createBeanResult();
        assertThrows(RuntimeException.class, () -> {
            beanInfo.getPropInfo("value").setPropValue(result, "not-a-number");
            beanInfo.finishBeanResult(result);
        });
    }

    private static ParserUtil.BeanInfo info(final Class<?> type, final boolean asm) {
        final ParserUtil.BeanInfo result = ParserUtil.getBeanInfo(type, asm);
        assertEquals(asm ? ParserUtil.ASMPropInfo.class : ParserUtil.PropInfo.class, result.getPropInfo("value").getClass());
        return result;
    }
}
