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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;

@Tag("new-test")
public class PropInfo100Test extends TestBase {

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
        Assertions.assertTrue(idProp.isMarkedToId);
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
        CharacterWriter writer = Objectory.createBufferedJSONWriter();
        JSONXMLSerializationConfig<?> config = JSC.create();
        config.setStringQuotation('"');

        ParserUtil.PropInfo nameProp = beanInfo.getPropInfo("name");
        ParserUtil.PropInfo dateProp = beanInfo.getPropInfo("dateField");
        ParserUtil.PropInfo numberProp = beanInfo.getPropInfo("numberField");

        nameProp.writePropValue(writer, "TestName", config);
        String result = writer.toString();
        Assertions.assertTrue(result.contains("TestName"));

        Objectory.recycle(writer);
        writer = Objectory.createBufferedJSONWriter();
        Date date = new Date();
        dateProp.writePropValue(writer, date, config);
        result = writer.toString();
        Assertions.assertNotNull(result);

        Objectory.recycle(writer);
        writer = Objectory.createBufferedJSONWriter();
        numberProp.writePropValue(writer, 1234.56, config);
        result = writer.toString();
        Assertions.assertTrue(result.contains("1,234.56"));

        Objectory.recycle(writer);
        writer = Objectory.createBufferedJSONWriter();
        nameProp.writePropValue(writer, null, config);
        result = writer.toString();
        Assertions.assertEquals("null", result);

        Objectory.recycle(writer);
    }

    @Test
    public void testWritePropValueWithJsonRawValue() throws IOException {
        CharacterWriter writer = Objectory.createBufferedJSONWriter();
        JSONXMLSerializationConfig<?> config = JSC.create();

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
        Assertions.assertEquals(2, jsonXmlField.alias().length);
    }

    @Test
    public void testDateTimeProperties() {
        testDateTimeType("dateField", new Date());
        testDateTimeType("timestampField", new Timestamp(System.currentTimeMillis()));
        testDateTimeType("calendarField", Calendar.getInstance());
        testDateTimeType("localDateTimeField", LocalDateTime.now());
        testDateTimeType("localDateField", LocalDate.now());
        testDateTimeType("localTimeField", LocalTime.now());
        testDateTimeType("zonedDateTimeField", ZonedDateTime.now());
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

    @JsonXmlConfig(namingPolicy = NamingPolicy.LOWER_CAMEL_CASE)
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

        @JsonXmlField(alias = { "alias1", "alias2" })
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
}
