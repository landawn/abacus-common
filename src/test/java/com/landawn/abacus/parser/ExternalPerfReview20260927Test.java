package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.N;

/**
 * Tests for the 2026-09-27 external performance review in the parser package: the StAX bean reader no longer joins the text
 * fragments of an unknown property. The {@code PropInfo.setPropValue} tests pin how type-mismatched values are converted on
 * the reflective and reflectasm paths.
 */
public class ExternalPerfReview20260927Test extends TestBase {

    public static class FieldBean {
        public int intValue;
        public double doubleValue;
        public Long longObject;
        public String text;
    }

    // No field is named after the properties, so both PropInfo kinds write through the setters.
    public static class SetterBean {
        private int iv;
        private double dv;

        public int getIntValue() {
            return iv;
        }

        public void setIntValue(final int intValue) {
            this.iv = intValue;
        }

        public double getDoubleValue() {
            return dv;
        }

        public void setDoubleValue(final double doubleValue) {
            if (doubleValue < 0) {
                throw new IllegalStateException("negative: " + doubleValue);
            }

            this.dv = doubleValue;
        }
    }

    private static PropInfo reflective(final Class<?> cls, final String propName) {
        final PropInfo propInfo = new BeanInfo(cls, cls, false).getPropInfo(propName);
        assertFalse(propInfo instanceof ParserUtil.ASMPropInfo);
        return propInfo;
    }

    private static PropInfo direct(final Class<?> cls, final String propName) {
        final PropInfo propInfo = new BeanInfo(cls, cls, true).getPropInfo(propName);
        assertTrue(propInfo instanceof ParserUtil.ASMPropInfo);
        return propInfo;
    }

    // reflectasm casts rather than widening, so a Float always reaches the double field through N.convert.
    @Test
    public void directFieldFloatToDoubleAlwaysConverts() {
        final PropInfo propInfo = direct(FieldBean.class, "doubleValue");
        final FieldBean bean = new FieldBean();
        final double expected = N.convert(1.1f, double.class);

        for (int i = 0; i < 250; i++) {
            propInfo.setPropValue(bean, 1.1f);
            assertEquals(expected, bean.doubleValue, "call " + i);
        }
    }

    @Test
    public void persistentMismatchIsConvertedOnEveryCall() {
        for (final PropInfo propInfo : new PropInfo[] { reflective(FieldBean.class, "intValue"), direct(FieldBean.class, "intValue") }) {
            final FieldBean bean = new FieldBean();

            for (int i = 0; i < 300; i++) {
                propInfo.setPropValue(bean, String.valueOf(i));
                assertEquals(i, bean.intValue);
            }

            propInfo.setPropValue(bean, (short) 7);
            assertEquals(7, bean.intValue);
            propInfo.setPropValue(bean, null);
            assertEquals(0, bean.intValue);
        }

        for (final PropInfo propInfo : new PropInfo[] { reflective(SetterBean.class, "intValue"), direct(SetterBean.class, "intValue") }) {
            final SetterBean bean = new SetterBean();

            for (int i = 0; i < 300; i++) {
                propInfo.setPropValue(bean, Long.valueOf(i));
                assertEquals(i, bean.getIntValue());
            }
        }
    }

    @Test
    public void referenceTypeMismatchIsConverted() {
        for (final PropInfo propInfo : new PropInfo[] { reflective(FieldBean.class, "longObject"), direct(FieldBean.class, "longObject") }) {
            final FieldBean bean = new FieldBean();

            for (int i = 0; i < 150; i++) {
                propInfo.setPropValue(bean, i);
                assertEquals(Long.valueOf(i), bean.longObject);
            }

            propInfo.setPropValue(bean, null);
            assertNull(bean.longObject);
        }

        final PropInfo textProp = reflective(FieldBean.class, "text");
        final FieldBean bean = new FieldBean();
        textProp.setPropValue(bean, 12);
        assertEquals("12", bean.text);
    }

    public static class ReferenceBean {
        public Runnable task;
        public java.util.List<String> list;
        public Number number;
    }

    // Values no conversion can make fit: the conversion's own exception, the same on both PropInfo kinds.
    @Test
    public void unconvertibleReferenceValues() {
        for (final boolean asm : new boolean[] { false, true }) {
            final PropInfo task = asm ? direct(ReferenceBean.class, "task") : reflective(ReferenceBean.class, "task");
            final PropInfo list = asm ? direct(ReferenceBean.class, "list") : reflective(ReferenceBean.class, "list");
            final PropInfo number = asm ? direct(ReferenceBean.class, "number") : reflective(ReferenceBean.class, "number");
            final ReferenceBean bean = new ReferenceBean();

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> task.setPropValue(bean, "x"));
            assertEquals("Conversion returned java.lang.String for java.lang.Runnable", e.getMessage());
            assertThrows(UnsupportedOperationException.class, () -> number.setPropValue(bean, "1"));

            list.setPropValue(bean, "x");
            assertEquals(java.util.List.of("x"), bean.list);
            number.setPropValue(bean, 1.5);
            assertEquals(1.5, bean.number);
            assertNull(bean.task);
        }
    }

    // A setter's own exception is not a type mismatch: it propagates, and the value is not retried.
    @Test
    public void setterExceptionStillPropagates() {
        for (final PropInfo propInfo : new PropInfo[] { reflective(SetterBean.class, "doubleValue"), direct(SetterBean.class, "doubleValue") }) {
            final SetterBean bean = new SetterBean();
            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> propInfo.setPropValue(bean, -1.0));
            assertEquals("negative: -1.0", e.getMessage());
            assertThrows(IllegalStateException.class, () -> propInfo.setPropValue(bean, "-2"));
            assertEquals(0.0, bean.getDoubleValue());
        }
    }

    // ---- StAX: text of an unknown property is skipped, not joined ----

    public static class XmlBean {
        private int id;
        private String name;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    @Test
    public void staxUnknownPropertyWithMultiFragmentText() {
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        final String big = "x".repeat(20_000);
        final String[] xmls = { "<xmlBean><id>1</id><unknown>abc&amp;def<![CDATA[ghi]]>jkl" + big + "</unknown><name>n&amp;m</name></xmlBean>",
                "<xmlBean><unknown><![CDATA[]]></unknown><id>1</id><name>n&amp;m</name></xmlBean>",
                "<xmlBean><id>1</id><unknown> &#10; <![CDATA[ ]]> <a>1</a><b><c>2</c></b></unknown><name>n&amp;m</name></xmlBean>",
                "<xmlBean><id>1</id><unknown>\n  <a>q&amp;r</a>\n</unknown><name>n&amp;m</name><unknown2>tail</unknown2></xmlBean>" };

        for (final String xml : xmls) {
            final XmlBean bean = parser.deserialize(xml, XmlBean.class);
            assertEquals(1, bean.getId(), xml);
            assertEquals("n&m", bean.getName(), xml);
        }

        // a known property after a large unknown multi-fragment value still reads its own text only
        final XmlBean bean = parser.deserialize("<xmlBean><unknown>a&amp;" + big + "</unknown><name>p&amp;q<![CDATA[r]]></name><id>2</id></xmlBean>",
                XmlBean.class);
        assertEquals("p&qr", bean.getName());
        assertEquals(2, bean.getId());
    }
}
