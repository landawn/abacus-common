package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.util.N;

/**
 * Field/accessor precedence of bean properties, with the reflectasm fast path ({@link ParserUtil.ASMPropInfo}) enabled
 * and disabled ({@link ParserUtil.PropInfo}).
 *
 * <p>The contract (see {@link AccessFieldByMethod}): a property is read and written through its field by default, and
 * through its getter/setter only when the field cannot be used or {@code @AccessFieldByMethod} demands it - a getter or
 * setter that "must not be bypassed" is never bypassed. Both modes must follow it; the fast path used to prefer the
 * getter/setter for every non-public field, so a bean's JSON depended on whether reflectasm was on the classpath.</p>
 */
@Tag("unit")
public class PropInfoFieldAccessPrecedenceTest extends TestBase {

    /** Result of one read and one write through a property, observed on the backing field. */
    private record Observed(Object read, Object fieldAfterWrite) {
    }

    private static final Observed FIELD_FIRST = new Observed("field", "json");

    @Test
    public void privateFieldIsReadAndWrittenDirectlyInBothModes() throws Exception {
        for (final boolean asm : modes()) {
            assertEquals(FIELD_FIRST, observe(PublicBaseBean.class, PublicBase.class, asm), "public base, asm=" + asm);
            assertEquals(FIELD_FIRST, observe(HiddenBaseBean.class, HiddenBase.class, asm), "hidden base, asm=" + asm);
            assertEquals(FIELD_FIRST, observe(TransformingBean.class, TransformingBean.class, asm), "transforming accessors, asm=" + asm);
        }
    }

    @Test
    public void accessFieldByMethodIsHonoredInBothModes() throws Exception {
        final Observed viaAccessors = new Observed("getter:field", "setter:json");

        for (final boolean asm : modes()) {
            assertEquals(viaAccessors, observe(AnnotatedTypeBean.class, AnnotatedTypeBean.class, asm), "type-level, asm=" + asm);
            assertEquals(viaAccessors, observe(AnnotatedFieldBean.class, AnnotatedFieldBean.class, asm), "field-level, asm=" + asm);
        }
    }

    @Test
    public void publicFieldIsReadAndWrittenDirectlyInBothModes() throws Exception {
        for (final boolean asm : modes()) {
            assertEquals(FIELD_FIRST, observe(PublicFieldBean.class, PublicFieldBean.class, asm), "asm=" + asm);
        }
    }

    @Test
    public void jsonRoundTripIsFieldFirst() throws Exception {
        // N.toJson/N.fromJson use the cached default metadata: the fast path exactly when reflectasm is available.
        assertEquals("{\"value\": \"field\"}", N.toJson(new HiddenBaseBean()));
        assertEquals("{\"value\": \"field\"}", N.toJson(new PublicBaseBean()));
        assertEquals("{\"value\": \"field\"}", N.toJson(new TransformingBean()));
        assertEquals("json", fieldValue(HiddenBase.class, N.fromJson("{\"value\":\"json\"}", HiddenBaseBean.class)));
        assertEquals("json", fieldValue(TransformingBean.class, N.fromJson("{\"value\":\"json\"}", TransformingBean.class)));

        assertEquals("{\"value\": \"getter:field\"}", N.toJson(new AnnotatedTypeBean()));
        assertEquals("setter:json", fieldValue(AnnotatedTypeBean.class, N.fromJson("{\"value\":\"json\"}", AnnotatedTypeBean.class)));
    }

    @SuppressWarnings("deprecation")
    private static Observed observe(final Class<?> beanClass, final Class<?> fieldOwner, final boolean asm) throws Exception {
        final ParserUtil.PropInfo propInfo = ParserUtil.getBeanInfo(beanClass, asm).getPropInfo("value");
        assertEquals(asm, propInfo instanceof ParserUtil.ASMPropInfo, beanClass.getSimpleName() + " asm=" + asm);

        final Object read = propInfo.getPropValue(beanClass.getDeclaredConstructor().newInstance());
        final Object written = beanClass.getDeclaredConstructor().newInstance();
        propInfo.setPropValue(written, "json");

        return new Observed(read, fieldValue(fieldOwner, written));
    }

    private static Object fieldValue(final Class<?> fieldOwner, final Object bean) throws Exception {
        final Field field = fieldOwner.getDeclaredField("value");
        field.setAccessible(true);
        return field.get(bean);
    }

    private static List<Boolean> modes() {
        final List<Boolean> modes = new ArrayList<>(List.of(false));

        if (ASMUtil.isASMAvailable()) {
            modes.add(true);
        }

        return modes;
    }

    public interface Named {
        default String getValue() {
            return "getter";
        }

        default void setValue(final String value) {
            // deliberately ignores the value
        }
    }

    static class HiddenBase implements Named {
        private String value = "field";
    }

    public static class HiddenBaseBean extends HiddenBase {
    }

    public static class PublicBase implements Named {
        private String value = "field";
    }

    public static class PublicBaseBean extends PublicBase {
    }

    public static class TransformingBean {
        private String value = "field";

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(final String value) {
            this.value = "setter:" + value;
        }
    }

    @AccessFieldByMethod
    public static class AnnotatedTypeBean {
        private String value = "field";

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(final String value) {
            this.value = "setter:" + value;
        }
    }

    public static class AnnotatedFieldBean {
        @AccessFieldByMethod
        private String value = "field";

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(final String value) {
            this.value = "setter:" + value;
        }
    }

    public static class PublicFieldBean {
        public String value = "field";

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(final String value) {
            this.value = "setter:" + value;
        }
    }
}
