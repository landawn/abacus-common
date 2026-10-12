package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;

@Tag("unit")
public class ParserUtilNonPublicBeanTest extends TestBase {
    @Test
    public void privateBeanPropertiesRoundTrip() {
        final HiddenBean bean = new HiddenBean();
        bean.setValue("first");

        assertEquals("{\"value\": \"first\"}", N.toJson(bean));
        assertEquals("second", N.fromJson("{\"value\":\"second\"}", HiddenBean.class).getValue());
    }

    @Test
    public void inheritedFieldsFromNonPublicClassesRoundTrip() {
        final PublicChild bean = new PublicChild();
        bean.value = "first";

        assertEquals("{\"value\": \"first\"}", N.toJson(bean));
        assertEquals("second", N.fromJson("{\"value\":\"second\"}", PublicChild.class).value);
    }

    @Test
    public void publicBeanPropertiesStillRoundTrip() {
        final PublicBean bean = new PublicBean();
        bean.setValue("first");

        assertEquals("{\"value\": \"first\"}", N.toJson(bean));
        assertEquals("second", N.fromJson("{\"value\":\"second\"}", PublicBean.class).getValue());
    }

    @Test
    public void publicPropertiesWithNonPublicTypesRoundTrip() {
        final PublicBeanWithHiddenSetterType bean = new PublicBeanWithHiddenSetterType();
        bean.setColor(HiddenColor.RED);

        assertEquals("{\"color\": \"RED\"}", N.toJson(bean));
        assertEquals(HiddenColor.GREEN, N.fromJson("{\"color\":\"GREEN\"}", PublicBeanWithHiddenSetterType.class).getColor());

        final PublicBeanWithHiddenFieldType fieldBean = new PublicBeanWithHiddenFieldType();
        fieldBean.color = HiddenColor.RED;

        assertEquals("{\"color\": \"RED\"}", N.toJson(fieldBean));
        assertEquals(HiddenColor.GREEN, N.fromJson("{\"color\":\"GREEN\"}", PublicBeanWithHiddenFieldType.class).color);
    }

    @Test
    public void fieldAccessorIsGeneratedOnlyForFieldsItCanServe() {
        assumeTrue(ASMUtil.isASMAvailable());

        // A private backing field uses PropInfo's field access, so no generated field accessor is needed for it.
        final ParserUtil.PropInfo privateBacked = ParserUtil.getBeanInfo(PublicBean.class).getPropInfo("value");
        assertInstanceOf(ParserUtil.ASMPropInfo.class, privateBacked);
        assertNull(((ParserUtil.ASMPropInfo) privateBacked).fieldAccess);
        assertEquals(-1, ((ParserUtil.ASMPropInfo) privateBacked).fieldAccessIndex);

        // A public non-final field is read and written through it directly.
        final ParserUtil.PropInfo publicField = ParserUtil.getBeanInfo(PublicFieldBean.class).getPropInfo("value");
        assertInstanceOf(ParserUtil.ASMPropInfo.class, publicField);
        assertNotNull(((ParserUtil.ASMPropInfo) publicField).fieldAccess);

        final PublicFieldBean bean = new PublicFieldBean();
        bean.value = "first";
        assertEquals("{\"value\": \"first\"}", N.toJson(bean));
        assertEquals("second", N.fromJson("{\"value\":\"second\"}", PublicFieldBean.class).value);
    }

    public static class PublicFieldBean {
        public String value;
    }

    enum HiddenColor {
        RED, GREEN
    }

    public static class PublicBeanWithHiddenSetterType {
        private HiddenColor color;

        public HiddenColor getColor() {
            return color;
        }

        public void setColor(final HiddenColor color) {
            this.color = color;
        }
    }

    public static class PublicBeanWithHiddenFieldType {
        public HiddenColor color;
    }

    private static class HiddenBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    private static class HiddenParent {
        public String value;
    }

    public static class PublicChild extends HiddenParent {
    }

    public static class PublicBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }
}
