package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

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
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.util.CharacterWriter;
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
    public void testPropInfoConstructor() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");

        Assertions.assertEquals("id", idProp.name);
        Assertions.assertEquals(Long.class, idProp.clazz);
        Assertions.assertNotNull(idProp.type);
        Assertions.assertNotNull(idProp.field);
        Assertions.assertNotNull(idProp.getMethod);
        Assertions.assertNotNull(idProp.setMethod);
        Assertions.assertTrue(idProp.isMarkedAsId);
        Assertions.assertFalse(idProp.isTransient);
    }

    @Test
    public void testGetPropValue() {
        testBean.setId(123L);
        testBean.setName("TestName");
        testBean.setDateField(new Date());

        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");

        Long id = idProp.getPropValue(testBean);
        Assertions.assertEquals(123L, id);

        String name = nameProp.getPropValue(testBean);
        Assertions.assertEquals("TestName", name);

        Date date = dateProp.getPropValue(testBean);
        Assertions.assertNotNull(date);
    }

    @Test
    public void testSetPropValue() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");

        idProp.setPropValue(testBean, 456L);
        Assertions.assertEquals(456L, testBean.getId());

        nameProp.setPropValue(testBean, "UpdatedName");
        Assertions.assertEquals("UpdatedName", testBean.getName());

        idProp.setPropValue(testBean, "789");
        Assertions.assertEquals(789L, testBean.getId());
    }

    @Test
    public void testSetPropValueWithJsonRawValue() {
        ParserUtil.PropInfo jsonRawProp = beanInfo.getPropInfo("jsonRawField");

        TestObject obj = new TestObject();
        obj.value = "TestValue";
        jsonRawProp.setPropValue(testBean, obj);

        String jsonRawValue = testBean.getJsonRawField();
        Assertions.assertNotNull(jsonRawValue);
        Assertions.assertTrue(jsonRawValue.contains("TestValue"));
    }

    @Test
    public void testReadPropValueWithDateFormat() {
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");
        ParserUtil.PropInfo longDateProp = beanInfo.getPropInfo("longDateField");

        Date date = (Date) dateProp.readPropValue("2023-12-25");
        Assertions.assertNotNull(date);

        long timestamp = System.currentTimeMillis();
        Date longDate = (Date) longDateProp.readPropValue(String.valueOf(timestamp));
        Assertions.assertNotNull(longDate);
        Assertions.assertEquals(timestamp, longDate.getTime());
    }

    @Test
    public void testReadPropValueWithNumberFormat() {
        ParserUtil.PropInfo numberProp = beanInfo.getPropInfo("numberField");

        Double number = (Double) numberProp.readPropValue("1234.56");
        Assertions.assertNotNull(number);
        Assertions.assertEquals(1234.56, number, 0.01);
    }

    @Test
    public void testWritePropValue() throws IOException {
        CharacterWriter writer = Objectory.createBufferedJsonWriter();
        JsonXmlSerConfig<?> config = JsonSerConfig.create();
        config.setStringQuotation('"');

        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");
        ParserUtil.PropInfo numberProp = beanInfo.getPropInfo("numberField");

        nameProp.writePropValue(writer, "TestName", config);
        String result = writer.toString();
        Assertions.assertTrue(result.contains("TestName"));

        Objectory.recycle(writer);
        writer = Objectory.createBufferedJsonWriter();
        Date date = new Date();
        dateProp.writePropValue(writer, date, config);
        result = writer.toString();
        Assertions.assertNotNull(result);

        Objectory.recycle(writer);
        writer = Objectory.createBufferedJsonWriter();
        numberProp.writePropValue(writer, 1234.56, config);
        result = writer.toString();
        Assertions.assertTrue(result.contains("1,234.56"));

        Objectory.recycle(writer);
        writer = Objectory.createBufferedJsonWriter();
        nameProp.writePropValue(writer, null, config);
        result = writer.toString();
        Assertions.assertEquals("null", result);

        Objectory.recycle(writer);
    }

    @Test
    public void testWritePropValueWithJsonRawValue() throws IOException {
        CharacterWriter writer = Objectory.createBufferedJsonWriter();
        JsonXmlSerConfig<?> config = JsonSerConfig.create();

        ParserUtil.PropInfo jsonRawProp = beanInfo.getPropInfo("jsonRawField");

        jsonRawProp.writePropValue(writer, "{\"key\":\"value\"}", config);
        String result = writer.toString();
        Assertions.assertEquals("{\"key\":\"value\"}", result);
        Objectory.recycle(writer);
    }

    @Test
    public void testIsAnnotationPresent() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo columnProp = beanInfo.getPropInfo("columnField");
        ParserUtil.PropInfo transientProp = beanInfo.getPropInfo("transientField");

        Assertions.assertTrue(idProp.isAnnotationPresent(Id.class));
        Assertions.assertFalse(idProp.isAnnotationPresent(Column.class));

        Assertions.assertTrue(columnProp.isAnnotationPresent(Column.class));
        Assertions.assertFalse(columnProp.isAnnotationPresent(Id.class));

        Assertions.assertTrue(transientProp.isAnnotationPresent(Transient.class));
    }

    @Test
    public void testGetAnnotation() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        ParserUtil.PropInfo columnProp = beanInfo.getPropInfo("columnField");
        ParserUtil.PropInfo aliasedProp = beanInfo.getPropInfo("aliasedField");

        Id idAnnotation = idProp.getAnnotation(Id.class);
        Assertions.assertNotNull(idAnnotation);

        Column columnAnnotation = columnProp.getAnnotation(Column.class);
        Assertions.assertNotNull(columnAnnotation);
        Assertions.assertEquals("db_column", columnAnnotation.value());

        JsonXmlField jsonXmlField = aliasedProp.getAnnotation(JsonXmlField.class);
        Assertions.assertNotNull(jsonXmlField);
        Assertions.assertEquals(2, jsonXmlField.aliases().length);
    }

    @Test
    public void testDateTimeProperties() {
        assertDoesNotThrow(() -> {
            testDateTimeType("dateField", new Date());
            testDateTimeType("timestampField", new Timestamp(System.currentTimeMillis()));
            testDateTimeType("calendarField", Calendar.getInstance());
            testDateTimeType("localDateTimeField", LocalDateTime.now());
            testDateTimeType("localDateField", LocalDate.now());
            testDateTimeType("localTimeField", LocalTime.now());
            testDateTimeType("zonedDateTimeField", ZonedDateTime.now());
        });
    }

    private void testDateTimeType(String propName, Object value) {
        ParserUtil.PropInfo propInfo = beanInfo.getPropInfo(propName);
        Assertions.assertNotNull(propInfo);

        propInfo.setPropValue(testBean, value);
        Object retrievedValue = propInfo.getPropValue(testBean);
        Assertions.assertNotNull(retrievedValue);
    }

    @Test
    public void testHashCodeAndEquals() {
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
    public void testToString() {
        ParserUtil.PropInfo prop = beanInfo.getPropInfo("id");
        Assertions.assertEquals("id", prop.toString());
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

    @Test
    public void testSetPropValue_withNullValue() {
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        nameProp.setPropValue(testBean, null);
        Assertions.assertNull(testBean.getName());
    }

    @Test
    public void testSetPropValue_withNumericStringConversion() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        idProp.setPropValue(testBean, "100");
        Assertions.assertEquals(100L, testBean.getId());
    }

    @Test
    public void testSetPropValue_withDirectLongValue() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        idProp.setPropValue(testBean, 200L);
        Assertions.assertEquals(200L, testBean.getId());
    }

    @Test
    public void testGetPropValue_returnsNull_whenNotSet() {
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        Assertions.assertNull(nameProp.getPropValue(testBean));
    }

    @Test
    public void testGetPropValue_returnsCorrectValue() {
        testBean.setName("GetTest");
        testBean.setId(999L);
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        Assertions.assertEquals("GetTest", nameProp.getPropValue(testBean));
        Assertions.assertEquals(999L, (Long) idProp.getPropValue(testBean));
    }

    @Test
    public void testReadPropValue_stringType() {
        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        String result = (String) nameProp.readPropValue("hello");
        Assertions.assertEquals("hello", result);
    }

    @Test
    public void testReadPropValue_longType() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        Long result = (Long) idProp.readPropValue("12345");
        Assertions.assertEquals(12345L, result);
    }

    @Test
    public void testReadPropValue_withNullFormatField() {
        ParserUtil.PropInfo columnProp = beanInfo.getPropInfo("columnField");
        Assertions.assertFalse(columnProp.hasFormat);
        String result = (String) columnProp.readPropValue("colValue");
        Assertions.assertEquals("colValue", result);
    }

    @Test
    public void testWritePropValue_withNullValue() throws java.io.IOException {
        com.landawn.abacus.util.CharacterWriter writer = Objectory.createBufferedJsonWriter();
        try {
            ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
            idProp.writePropValue(writer, null, JsonSerConfig.create());
            String result = writer.toString();
            Assertions.assertEquals("null", result);
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testWritePropValue_stringTypeNoFormat() throws java.io.IOException {
        com.landawn.abacus.util.CharacterWriter writer = Objectory.createBufferedJsonWriter();
        try {
            ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
            nameProp.writePropValue(writer, "plainValue", JsonSerConfig.create());
            String result = writer.toString();
            Assertions.assertTrue(result.contains("plainValue"));
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testPropInfo_isTransientField() {
        ParserUtil.PropInfo transientProp = beanInfo.getPropInfo("transientField");
        Assertions.assertNotNull(transientProp);
        Assertions.assertTrue(transientProp.isTransient);
    }

    @Test
    public void testPropInfo_isMarkedAsReadOnlyId() {
        ParserUtil.PropInfo readOnlyIdProp = beanInfo.getPropInfo("readOnlyId");
        Assertions.assertNotNull(readOnlyIdProp);
        Assertions.assertTrue(readOnlyIdProp.isMarkedAsReadOnlyId);
    }

    @Test
    public void testGetAnnotations_nonNullForAnnotatedField() {
        ParserUtil.PropInfo idProp = beanInfo.getPropInfo("id");
        Assertions.assertNotNull(idProp.annotations);
        Assertions.assertFalse(idProp.annotations.isEmpty());
    }

    @Test
    public void testPropInfoHasFormat_forDateField() {
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");
        Assertions.assertTrue(dateProp.hasFormat);
        Assertions.assertNotNull(dateProp.dateFormat);
    }

    @Test
    public void testPropInfoHasFormat_forNumberField() {
        ParserUtil.PropInfo numProp = beanInfo.getPropInfo("numberField");
        Assertions.assertTrue(numProp.hasFormat);
        Assertions.assertNotNull(numProp.numberFormat);
    }

    @Test
    public void testBeanInfoWithEnumField() {
        ParserUtil.BeanInfo enumBeanInfo = ParserUtil.getBeanInfo(BeanWithEnum.class);
        ParserUtil.PropInfo statusProp = enumBeanInfo.getPropInfo("status");
        Assertions.assertNotNull(statusProp);
        Assertions.assertNotNull(statusProp.type);
    }

    @Test
    public void testSetAndGetPropValue_allDateTimeTypes() {
        java.util.Date now = new java.util.Date();
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");
        dateProp.setPropValue(testBean, now);
        Assertions.assertEquals(now, dateProp.getPropValue(testBean));

        java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
        ParserUtil.PropInfo tsProp = beanInfo.getPropInfo("timestampField");
        tsProp.setPropValue(testBean, ts);
        Assertions.assertEquals(ts, tsProp.getPropValue(testBean));
    }

    @Test
    public void testWritePropValue_withDateFormatAndNoQuote() throws java.io.IOException {
        com.landawn.abacus.util.CharacterWriter writer = Objectory.createBufferedJsonWriter();
        try {
            ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");
            // No quotation in config
            JsonXmlSerConfig<?> noQuoteConfig = JsonSerConfig.create().setStringQuotation((char) 0);
            java.util.Date date = new java.util.Date();
            dateProp.writePropValue(writer, date, noQuoteConfig);
            String result = writer.toString();
            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.isEmpty());
        } finally {
            Objectory.recycle(writer);
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

    // TODO: Remaining PropInfo gaps are low-level field-handle/object-array assignment branches that need synthetic BeanInfo wiring not exposed through normal property APIs.

}
