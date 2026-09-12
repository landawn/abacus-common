package com.landawn.abacus.parser;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;

public class PropInfoTest extends TestBase {

    private ParserUtil.BeanInfo beanInfo;
    private TestBean testBean;

    @BeforeEach
    public void setup() {
        beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        testBean = new TestBean();
    }

    @Test
    public void testConstructorAndFlags() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        Assertions.assertEquals("id", idProp.name);
        Assertions.assertEquals(Long.class, idProp.clazz);
        Assertions.assertNotNull(idProp.type);
        Assertions.assertNotNull(idProp.field);
        Assertions.assertNotNull(idProp.getMethod);
        Assertions.assertNotNull(idProp.setMethod);
        Assertions.assertTrue(idProp.isMarkedAsId);
        Assertions.assertFalse(idProp.isTransient);
        Assertions.assertEquals("id", idProp.toString());
        Assertions.assertNotNull(idProp.annotations);
        Assertions.assertFalse(idProp.annotations.isEmpty());

        Assertions.assertTrue(beanInfo.getPropInfo("transientField").isTransient);
        Assertions.assertTrue(beanInfo.getPropInfo("readOnlyId").isMarkedAsReadOnlyId);
        Assertions.assertTrue(beanInfo.getPropInfo("dateField").hasFormat);
        Assertions.assertNotNull(beanInfo.getPropInfo("dateField").dateFormat);
        Assertions.assertTrue(beanInfo.getPropInfo("numberField").hasFormat);
        Assertions.assertNotNull(beanInfo.getPropInfo("numberField").numberFormat);
        Assertions.assertFalse(beanInfo.getPropInfo("columnField").hasFormat);

        ParserUtil.PropInfo enumProp = ParserUtil.getBeanInfo(BeanWithEnum.class).getPropInfo("status");
        Assertions.assertNotNull(enumProp);
        Assertions.assertNotNull(enumProp.type);
    }

    @Test
    public void testGetAndSetPropValue() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");

        Assertions.assertNull(nameProp.getPropValue(testBean));
        testBean.setId(123L);
        testBean.setName("TestName");
        testBean.setDateField(new Date());
        Assertions.assertEquals(Long.valueOf(123L), idProp.getPropValue(testBean));
        Assertions.assertEquals("TestName", nameProp.getPropValue(testBean));
        Assertions.assertNotNull(dateProp.getPropValue(testBean));

        idProp.setPropValue(testBean, 456L);
        Assertions.assertEquals(456L, testBean.getId());
        nameProp.setPropValue(testBean, "UpdatedName");
        Assertions.assertEquals("UpdatedName", testBean.getName());
        idProp.setPropValue(testBean, "789");
        Assertions.assertEquals(789L, testBean.getId());
        nameProp.setPropValue(testBean, null);
        Assertions.assertNull(testBean.getName());
        idProp.setPropValue(testBean, "100");
        Assertions.assertEquals(100L, testBean.getId());
        idProp.setPropValue(testBean, 200L);
        Assertions.assertEquals(200L, testBean.getId());

        ParserUtil.PropInfo jsonRawProp = beanInfo.getPropInfo("jsonRawField");
        TestObject obj = new TestObject();
        obj.value = "TestValue";
        jsonRawProp.setPropValue(testBean, obj);
        Assertions.assertTrue(testBean.getJsonRawField().contains("TestValue"));

        Date now = new Date();
        dateProp.setPropValue(testBean, now);
        Assertions.assertEquals(now, dateProp.getPropValue(testBean));
        Timestamp ts = new Timestamp(System.currentTimeMillis());
        beanInfo.getPropInfo("timestampField").setPropValue(testBean, ts);
        Assertions.assertEquals(ts, beanInfo.getPropInfo("timestampField").getPropValue(testBean));

        dateProp.setPropValue(testBean, new Date());
        beanInfo.getPropInfo("timestampField").setPropValue(testBean, new Timestamp(System.currentTimeMillis()));
        beanInfo.getPropInfo("calendarField").setPropValue(testBean, Calendar.getInstance());
        beanInfo.getPropInfo("localDateTimeField").setPropValue(testBean, LocalDateTime.now());
        beanInfo.getPropInfo("localDateField").setPropValue(testBean, LocalDate.now());
        beanInfo.getPropInfo("localTimeField").setPropValue(testBean, LocalTime.now());
        beanInfo.getPropInfo("zonedDateTimeField").setPropValue(testBean, ZonedDateTime.now());
        Assertions.assertNotNull(dateProp.getPropValue(testBean));
        Assertions.assertNotNull(beanInfo.getPropInfo("zonedDateTimeField").getPropValue(testBean));
    }

    @Test
    public void testReadPropValue() {
        Assertions.assertNotNull(beanInfo.getPropInfo("dateField").readPropValue("2023-12-25"));
        long timestamp = System.currentTimeMillis();
        Date longDate = (Date) beanInfo.getPropInfo("longDateField").readPropValue(String.valueOf(timestamp));
        Assertions.assertEquals(timestamp, longDate.getTime());
        Assertions.assertEquals(1234.56, (Double) beanInfo.getPropInfo("numberField").readPropValue("1234.56"), 0.01);
        Assertions.assertEquals("hello", beanInfo.getPropInfo("name").readPropValue("hello"));
        Assertions.assertEquals(12345L, beanInfo.getPropInfo("id").readPropValue("12345"));
        Assertions.assertEquals("colValue", beanInfo.getPropInfo("columnField").readPropValue("colValue"));
    }

    @Test
    public void testWritePropValue() throws IOException {
        JsonXmlSerConfig<?> config = JsonSerConfig.create().setStringQuotation('"');
        CharacterWriter writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("name").writePropValue(writer, "TestName", config);
            Assertions.assertTrue(writer.toString().contains("TestName"));
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("dateField").writePropValue(writer, new Date(), config);
            Assertions.assertFalse(writer.toString().isEmpty());
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("numberField").writePropValue(writer, 1234.56, config);
            Assertions.assertTrue(writer.toString().contains("1,234.56"));
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("name").writePropValue(writer, null, config);
            Assertions.assertEquals("null", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("jsonRawField").writePropValue(writer, "{\"key\":\"value\"}", JsonSerConfig.create());
            Assertions.assertEquals("{\"key\":\"value\"}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("id").writePropValue(writer, null, JsonSerConfig.create());
            Assertions.assertEquals("null", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("name").writePropValue(writer, "plainValue", JsonSerConfig.create());
            Assertions.assertTrue(writer.toString().contains("plainValue"));
        } finally {
            Objectory.recycle(writer);
        }

        writer = Objectory.createBufferedJsonWriter();
        try {
            beanInfo.getPropInfo("dateField").writePropValue(writer, new Date(), JsonSerConfig.create().setStringQuotation((char) 0));
            Assertions.assertFalse(writer.toString().isEmpty());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testAnnotations() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo columnProp = beanInfo.getPropInfo("columnField");
        ParserUtil.PropInfo transientProp = beanInfo.getPropInfo("transientField");
        ParserUtil.PropInfo aliasedProp = beanInfo.getPropInfo("aliasedField");

        Assertions.assertTrue(idProp.isAnnotationPresent(Id.class));
        Assertions.assertFalse(idProp.isAnnotationPresent(Column.class));
        Assertions.assertTrue(columnProp.isAnnotationPresent(Column.class));
        Assertions.assertTrue(transientProp.isAnnotationPresent(Transient.class));
        Assertions.assertNotNull(idProp.getAnnotation(Id.class));
        Assertions.assertEquals("db_column", columnProp.getAnnotation(Column.class).value());
        Assertions.assertEquals(2, aliasedProp.getAnnotation(JsonXmlField.class).aliases().length);
    }

    @Test
    public void testEqualsHashCode() {
        ParserUtil.PropInfo prop1 = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo prop2 = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo prop3 = beanInfo.getPropInfo("name");
        Assertions.assertEquals(prop1.hashCode(), prop2.hashCode());
        Assertions.assertNotEquals(prop1.hashCode(), prop3.hashCode());
        Assertions.assertEquals(prop1, prop1);
        Assertions.assertEquals(prop1, prop2);
        Assertions.assertNotEquals(prop1, prop3);
        Assertions.assertNotEquals(prop1, null);
        Assertions.assertNotEquals(prop1, new Object());
    }

    @Test
    public void testBeanInfoSetPropValue_NestedCreatesIntermediate() {
        ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(NestedRootForPropInfo.class);
        NestedRootForPropInfo root = new NestedRootForPropInfo();
        Assertions.assertTrue(info.setPropValue(root, "child.name", "created", false));
        Assertions.assertEquals("created", root.getChild().getName());
    }

    @Test
    public void testGetPropValue_accessFieldByMethod() {
        ParserUtil.BeanInfo nonAsm = new ParserUtil.BeanInfo(AccessByMethodBean.class, AccessByMethodBean.class, false);
        Assertions.assertEquals("getter:field", nonAsm.getPropInfo("value").getPropValue(new AccessByMethodBean()));
        Assertions.assertEquals("getter:field", ParserUtil.getBeanInfo(AccessByMethodBean.class).getPropInfo("value").getPropValue(new AccessByMethodBean()));
        ParserUtil.BeanInfo direct = new ParserUtil.BeanInfo(DirectFieldBean.class, DirectFieldBean.class, false);
        Assertions.assertEquals("field", direct.getPropInfo("value").getPropValue(new DirectFieldBean()));
    }

    @JsonXmlConfig(namingPolicy = NamingPolicy.CAMEL_CASE)
    public static class TestBean {
        @Id
        private Long id;
        private String name;
        @JsonXmlField(dateFormat = "yyyy-MM-dd")
        private Date dateField;
        @JsonXmlField
        private Date longDateField;
        @JsonXmlField(numberFormat = "#,##0.00")
        private Double numberField;
        @Column("db_column")
        private String columnField;
        @Transient
        private String transientField;
        @JsonXmlField(aliases = { "alias1", "alias2" })
        private String aliasedField;
        @JsonXmlField(isJsonRawValue = true)
        private String jsonRawField;
        @ReadOnlyId
        private String readOnlyId;
        private Timestamp timestampField;
        private Calendar calendarField;
        private LocalDateTime localDateTimeField;
        private LocalDate localDateField;
        private LocalTime localTimeField;
        private ZonedDateTime zonedDateTimeField;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getDateField() {
            return dateField;
        }

        public void setDateField(Date dateField) {
            this.dateField = dateField;
        }

        public Date getLongDateField() {
            return longDateField;
        }

        public void setLongDateField(Date longDateField) {
            this.longDateField = longDateField;
        }

        public Double getNumberField() {
            return numberField;
        }

        public void setNumberField(Double numberField) {
            this.numberField = numberField;
        }

        public String getColumnField() {
            return columnField;
        }

        public void setColumnField(String columnField) {
            this.columnField = columnField;
        }

        public String getTransientField() {
            return transientField;
        }

        public void setTransientField(String transientField) {
            this.transientField = transientField;
        }

        public String getAliasedField() {
            return aliasedField;
        }

        public void setAliasedField(String aliasedField) {
            this.aliasedField = aliasedField;
        }

        public String getJsonRawField() {
            return jsonRawField;
        }

        public void setJsonRawField(String jsonRawField) {
            this.jsonRawField = jsonRawField;
        }

        public String getReadOnlyId() {
            return readOnlyId;
        }

        public void setReadOnlyId(String readOnlyId) {
            this.readOnlyId = readOnlyId;
        }

        public Timestamp getTimestampField() {
            return timestampField;
        }

        public void setTimestampField(Timestamp timestampField) {
            this.timestampField = timestampField;
        }

        public Calendar getCalendarField() {
            return calendarField;
        }

        public void setCalendarField(Calendar calendarField) {
            this.calendarField = calendarField;
        }

        public LocalDateTime getLocalDateTimeField() {
            return localDateTimeField;
        }

        public void setLocalDateTimeField(LocalDateTime localDateTimeField) {
            this.localDateTimeField = localDateTimeField;
        }

        public LocalDate getLocalDateField() {
            return localDateField;
        }

        public void setLocalDateField(LocalDate localDateField) {
            this.localDateField = localDateField;
        }

        public LocalTime getLocalTimeField() {
            return localTimeField;
        }

        public void setLocalTimeField(LocalTime localTimeField) {
            this.localTimeField = localTimeField;
        }

        public ZonedDateTime getZonedDateTimeField() {
            return zonedDateTimeField;
        }

        public void setZonedDateTimeField(ZonedDateTime zonedDateTimeField) {
            this.zonedDateTimeField = zonedDateTimeField;
        }
    }

    public static class TestObject {
        public String value;
    }

    public static class NestedRootForPropInfo {
        private NestedChildForPropInfo child;

        public NestedChildForPropInfo getChild() {
            return child;
        }

        public void setChild(final NestedChildForPropInfo child) {
            this.child = child;
        }
    }

    public static class NestedChildForPropInfo {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public enum Status {
        ACTIVE, INACTIVE
    }

    public static class BeanWithEnum {
        private Status status;

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
    }

    @AccessFieldByMethod
    public static class AccessByMethodBean {
        private String value = "field";

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    public static class DirectFieldBean {
        private String value = "field";

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    // --- review fixes 2026-09-06 (P3-08): readPropValue hands back the property's own type ---

    public static class FormattedNumbers {
        @JsonXmlField(numberFormat = "000")
        public Integer padded;
        @JsonXmlField(numberFormat = "0.00")
        public int primitive;
        @JsonXmlField(numberFormat = "#,##0.00")
        public java.math.BigDecimal money;
        @JsonXmlField(numberFormat = "#,##0")
        public java.math.BigInteger huge;
        @JsonXmlField(numberFormat = "0.00")
        public Double boxedDouble;
        @JsonXmlField(numberFormat = "0.00")
        public double primitiveDouble;
        @JsonXmlField(numberFormat = "0.00")
        public Number number;
    }

    @Test
    public void reviewFixes20260906_readPropValueConvertsFormattedNumbersToThePropertyType() {
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(FormattedNumbers.class);

        Object padded = beanInfo.getPropInfo("padded").readPropValue("001");
        Assertions.assertEquals(Integer.class, padded.getClass());
        Assertions.assertEquals(1, padded);

        Object primitive = beanInfo.getPropInfo("primitive").readPropValue("7.00");
        Assertions.assertEquals(Integer.class, primitive.getClass());
        Assertions.assertEquals(7, primitive);

        Object money = beanInfo.getPropInfo("money").readPropValue("12,345,678,901,234,567.89");
        Assertions.assertEquals(java.math.BigDecimal.class, money.getClass());
        Assertions.assertEquals(new java.math.BigDecimal("12345678901234567.89"), money);

        Object huge = beanInfo.getPropInfo("huge").readPropValue("123,456,789,012,345,678,901,234,567,890");
        Assertions.assertEquals(java.math.BigInteger.class, huge.getClass());
        Assertions.assertEquals(new java.math.BigInteger("123456789012345678901234567890"), huge);

        // Floating targets are not parsed as BigDecimal, so -0.0 keeps its sign, boxed or primitive.
        for (String prop : new String[] { "boxedDouble", "primitiveDouble" }) {
            Object negativeZero = beanInfo.getPropInfo(prop).readPropValue("-0.00");
            Assertions.assertEquals(Double.class, negativeZero.getClass(), prop);
            Assertions.assertEquals(Double.doubleToLongBits(-0.0), Double.doubleToLongBits((Double) negativeZero), prop);
        }

        // A Number-typed property receives DecimalFormat's own result class (Long for integral text).
        Object number = beanInfo.getPropInfo("number").readPropValue("1234.00");
        Assertions.assertEquals(Long.class, number.getClass());
        Assertions.assertEquals(1234L, number);

        Assertions.assertNull(beanInfo.getPropInfo("padded").readPropValue(null));
        Assertions.assertNull(beanInfo.getPropInfo("primitive").readPropValue(null));
    }

    @Test
    public void reviewFixes20260906_settingAFormattedReadNeverTakesTheRetryPath() {
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(FormattedNumbers.class);
        FormattedNumbers bean = new FormattedNumbers();

        for (int k = 0; k < 200; k++) {
            for (String[] pair : new String[][] { { "padded", "001" }, { "primitive", "7.00" }, { "money", "1,234.50" }, { "huge", "12" },
                    { "boxedDouble", "2.50" }, { "primitiveDouble", "2.50" }, { "number", "3.00" } }) {
                ParserUtil.PropInfo propInfo = beanInfo.getPropInfo(pair[0]);
                propInfo.setPropValue(bean, propInfo.readPropValue(pair[1]));
            }
        }

        Assertions.assertEquals(1, bean.padded);
        Assertions.assertEquals(7, bean.primitive);
        Assertions.assertEquals(new java.math.BigDecimal("1234.50"), bean.money);
        Assertions.assertEquals(java.math.BigInteger.valueOf(12), bean.huge);
        Assertions.assertEquals(2.5, bean.boxedDouble);
        Assertions.assertEquals(2.5, bean.primitiveDouble);
        Assertions.assertEquals(3L, bean.number);

        for (String prop : new String[] { "padded", "primitive", "money", "huge", "boxedDouble", "primitiveDouble", "number" }) {
            Assertions.assertEquals(0, beanInfo.getPropInfo(prop).failureCountForSetProp, prop);
        }
    }

    // --- review fixes 2026-09-06 (P3-09): a getter-only property is read-only, not an NPE ---

    @com.landawn.abacus.annotation.Entity
    public static class Computed {
        private String first;
        private String last;

        public String getFirst() {
            return first;
        }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getLast() {
            return last;
        }

        public void setLast(String last) {
            this.last = last;
        }

        /** Computed: getter only, no field, no setter. */
        public String getFullName() {
            return first + " " + last;
        }
    }

    public record ComputedRecord(String first, String last) {
        public String fullName() {
            return first + " " + last;
        }
    }

    @Test
    public void reviewFixes20260906_getterOnlyPropertyIsReadOnlyAndSerializeOnly() {
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(Computed.class);
        ParserUtil.PropInfo fullName = beanInfo.getPropInfo("fullName");
        ParserUtil.PropInfo first = beanInfo.getPropInfo("first");

        Computed bean = new Computed();
        bean.setFirst("Ada");
        bean.setLast("Lovelace");

        // Storing into it is a clear error instead of an NPE, for a value and for null alike.
        UnsupportedOperationException e = Assertions.assertThrows(UnsupportedOperationException.class, () -> fullName.setPropValue(bean, "x"));
        Assertions.assertTrue(e.getMessage().contains("fullName") && e.getMessage().contains("read-only"), e.getMessage());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fullName.setPropValue(bean, null));
        Assertions.assertEquals("Ada", bean.getFirst());

        Assertions.assertTrue(fullName.isReadOnlyProperty);
        Assertions.assertEquals(JsonXmlField.Direction.SERIALIZE_ONLY, fullName.jsonXmlExpose);
        Assertions.assertFalse(first.isReadOnlyProperty);
        Assertions.assertEquals(JsonXmlField.Direction.BOTH, first.jsonXmlExpose);

        // Still readable and still serialized.
        Assertions.assertEquals("Ada Lovelace", fullName.getPropValue(bean));
        Assertions.assertTrue(N.toJson(bean).contains("\"fullName\": \"Ada Lovelace\""), N.toJson(bean));

        // The name-based setter honours the ignore flag: skipped, or IllegalArgumentException.
        Assertions.assertFalse(beanInfo.setPropValue(bean, "fullName", "x", true));
        IllegalArgumentException iae = Assertions.assertThrows(IllegalArgumentException.class, () -> beanInfo.setPropValue(bean, "fullName", "x", false));
        Assertions.assertTrue(iae.getMessage().contains("read-only"), iae.getMessage());
        Assertions.assertThrows(IllegalArgumentException.class, () -> beanInfo.setPropValue(bean, "fullName", "x"));
        Assertions.assertTrue(beanInfo.setPropValue(bean, "first", "Grace", true));
        Assertions.assertEquals("Grace", bean.getFirst());

        // A record component is stored through the canonical constructor slot: never read-only.
        ParserUtil.BeanInfo recordInfo = ParserUtil.getBeanInfo(ComputedRecord.class);
        Assertions.assertFalse(recordInfo.getPropInfo("first").isReadOnlyProperty);
        Assertions.assertEquals(JsonXmlField.Direction.BOTH, recordInfo.getPropInfo("first").jsonXmlExpose);
        ComputedRecord record = N.fromJson("{\"first\": \"A\", \"last\": \"B\"}", ComputedRecord.class);
        Assertions.assertEquals("A B", record.fullName());
    }

    // ===================================== a builder-based bean whose builder omits a property

    /** Immutable, built through a builder that exposes {@code value} only; {@code derived} is computed by build(). */
    public static class OmittedByBuilder {
        private final String value;
        private final String derived;

        private OmittedByBuilder(final String value) {
            this.value = value;
            this.derived = value == null ? null : value.toUpperCase();
        }

        public String getValue() {
            return value;
        }

        public String getDerived() {
            return derived;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String value;

            public Builder value(final String value) {
                this.value = value;
                return this;
            }

            public OmittedByBuilder build() {
                return new OmittedByBuilder(value);
            }
        }
    }

    /** Builder-based, but with a NON-final field, so the bean's own field has a settable VarHandle. */
    public static class NonFinalByBuilder {
        private String value;

        private NonFinalByBuilder(final String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String value;

            public Builder value(final String value) {
                this.value = value;
                return this;
            }

            public NonFinalByBuilder build() {
                return new NonFinalByBuilder(value);
            }
        }
    }

    public record CoalesceRecord(int n, String s) {
    }

    /**
     * A builder-based bean may declare a property its builder does not accept. The write target is then the BUILDER
     * instance, so none of {@code setPropValue}'s writers applies - {@code field} belongs to the BEAN, and the final
     * {@code field.set(obj, ..)} fallback threw {@code IllegalArgumentException: Can not set final java.lang.String
     * field ..OmittedByBuilder.derived to ..OmittedByBuilder$Builder}. Such a property is skipped instead, which is
     * what lets the bean round-trip through JSON/XML and through the copy family.
     */
    @Test
    public void reviewFixes20260911_builderOmittedPropertyIsSkippedInsteadOfWrittenToTheBuilder() {
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(OmittedByBuilder.class);
        final ParserUtil.PropInfo derived = beanInfo.getPropInfo("derived");
        final ParserUtil.PropInfo value = beanInfo.getPropInfo("value");

        // The builder has a setter for "value" and none for "derived"; neither is written through the bean's field.
        Assertions.assertNull(derived.setMethod);
        Assertions.assertNotNull(value.setMethod);
        Assertions.assertEquals(OmittedByBuilder.Builder.class, value.setMethod.getDeclaringClass());
        Assertions.assertFalse(derived.isFieldSettable);
        Assertions.assertFalse(derived.isFieldHandleSettable);

        // It is NOT read-only: it is a full property and is serialized like any other.
        Assertions.assertFalse(derived.isReadOnlyProperty);
        Assertions.assertEquals(JsonXmlField.Direction.BOTH, derived.jsonXmlExpose);

        final OmittedByBuilder bean = OmittedByBuilder.builder().value("v").build();
        Assertions.assertEquals("{\"value\": \"v\", \"derived\": \"V\"}", N.toJson(bean));

        // Storing into it is a silent no-op on the builder - not an exception, and not a stray write.
        final Object builder = beanInfo.createBeanResult();
        Assertions.assertEquals(OmittedByBuilder.Builder.class, builder.getClass());
        derived.setPropValue(builder, "IGNORED");
        derived.setPropValue(builder, null);
        value.setPropValue(builder, "v");
        final OmittedByBuilder built = beanInfo.finishBeanResult(builder);
        Assertions.assertEquals("v", built.getValue());
        Assertions.assertEquals("V", built.getDerived());

        // The name-keyed setter reports the property as handled; the value is deliberately dropped.
        final Object builder2 = beanInfo.createBeanResult();
        Assertions.assertTrue(beanInfo.setPropValue(builder2, "derived", "IGNORED", true));
        Assertions.assertTrue(beanInfo.setPropValue(builder2, "derived", "IGNORED", false));
        Assertions.assertNull(beanInfo.<OmittedByBuilder> finishBeanResult(builder2).getDerived());

        // fromJson of a document that CARRIES the omitted property: the value is dropped, the bean still builds.
        final OmittedByBuilder fromJson = N.fromJson("{\"value\": \"v\", \"derived\": \"ZZZ\"}", OmittedByBuilder.class);
        Assertions.assertEquals("v", fromJson.getValue());
        Assertions.assertEquals("V", fromJson.getDerived());

        // ... which is exactly what makes the bean's own output readable back.
        final OmittedByBuilder roundTrip = N.fromJson(N.toJson(bean), OmittedByBuilder.class);
        Assertions.assertEquals("v", roundTrip.getValue());
        Assertions.assertEquals("V", roundTrip.getDerived());

        final OmittedByBuilder xmlRoundTrip = N.fromXml(N.toXml(bean), OmittedByBuilder.class);
        Assertions.assertEquals("v", xmlRoundTrip.getValue());
        Assertions.assertEquals("V", xmlRoundTrip.getDerived());

        // The copy family drives the same writer.
        Assertions.assertEquals("v", Beans.copyAs(bean, OmittedByBuilder.class).getValue());
        Assertions.assertEquals("V", Beans.copyAs(bean, OmittedByBuilder.class).getDerived());
        Assertions.assertEquals("v", Beans.mapToBean(Beans.beanToMap(bean, false), OmittedByBuilder.class).getValue());
        Assertions.assertEquals("V", Beans.mapToBean(Beans.beanToMap(bean, false), OmittedByBuilder.class).getDerived());

        // The canonical-constructor (non-builder) branch still coalesces a null into the primitive slot default.
        final CoalesceRecord rec = N.fromJson("{\"n\": null, \"s\": \"x\"}", CoalesceRecord.class);
        Assertions.assertEquals(0, rec.n());
        Assertions.assertEquals("x", rec.s());
    }

    /**
     * {@code isFieldSettable} excludes a builder-based property because {@code setPropValue}'s target is the BUILDER,
     * not the bean; {@code isFieldHandleSettable} has to do the same. It did not, so for a builder-based bean with a
     * NON-final field the {@code failureCountForSetProp > 100} retry branch preferred the bean's VarHandle and threw
     * {@code ClassCastException: Cannot cast ..NonFinalByBuilder$Builder to ..NonFinalByBuilder}. (Reflectasm, when
     * present, overrides {@code setPropValue} and never reads the VarHandle, so the flag itself is what is pinned.)
     */
    @Test
    public void reviewFixes20260911_builderBasedPropertyIsNeverWrittenThroughTheBeansVarHandle() {
        final ParserUtil.PropInfo nonFinal = ParserUtil.getBeanInfo(NonFinalByBuilder.class).getPropInfo("value");

        Assertions.assertNotNull(nonFinal.fieldHandle);
        Assertions.assertTrue(nonFinal.isFieldHandleGettable);
        Assertions.assertFalse(nonFinal.isFieldSettable);
        Assertions.assertFalse(nonFinal.isFieldHandleSettable);

        // A final-field builder bean has no settable VarHandle to begin with, so it cannot show the difference.
        Assertions.assertFalse(ParserUtil.getBeanInfo(OmittedByBuilder.class).getPropInfo("value").isFieldHandleSettable);

        // A plain mutable bean must keep its settable VarHandle.
        Assertions.assertTrue(ParserUtil.getBeanInfo(Computed.class).getPropInfo("first").isFieldHandleSettable);

        Assertions.assertEquals("{\"value\": \"v\"}", N.toJson(N.fromJson("{\"value\": \"v\"}", NonFinalByBuilder.class)));
    }

}
