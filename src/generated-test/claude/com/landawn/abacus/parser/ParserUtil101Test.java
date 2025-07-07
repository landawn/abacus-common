package com.landawn.abacus.parser;

import java.lang.reflect.Field;
import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.annotation.Column;
import com.landawn.abacus.annotation.Id;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.ReadOnlyId;
import com.landawn.abacus.annotation.Transient;
import com.landawn.abacus.annotation.Type.EnumBy;
import com.landawn.abacus.util.NamingPolicy;

import com.landawn.abacus.TestBase;


public class ParserUtil101Test extends TestBase {

    private TestBean testBean;
    private Field simpleField;
    private Field jsonXmlField;
    private Field columnField;
    private Field transientField;
    
    @BeforeEach
    public void setup() throws Exception {
        testBean = new TestBean();
        simpleField = TestBean.class.getDeclaredField("simpleName");
        jsonXmlField = TestBean.class.getDeclaredField("jsonFieldName");
        columnField = TestBean.class.getDeclaredField("columnName");
        transientField = TestBean.class.getDeclaredField("transientField");
    }

    @Test
    public void testGetBeanInfo() {
        // Test getting bean info for a valid bean class
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(TestBean.class);
        Assertions.assertNotNull(beanInfo);
        Assertions.assertEquals(TestBean.class, beanInfo.clazz);
        Assertions.assertEquals("TestBean", beanInfo.simpleClassName);
        
        // Test caching - should return the same instance
        ParserUtil.BeanInfo cachedBeanInfo = ParserUtil.getBeanInfo(TestBean.class);
        Assertions.assertSame(beanInfo, cachedBeanInfo);
    }
    
    @Test
    public void testGetBeanInfoInvalidClass() {
        // Test with non-bean class (no properties)
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ParserUtil.getBeanInfo(EmptyClass.class);
        });
    }
    
    @Test
    public void testRefreshBeanPropInfo() {
        // Get initial bean info
        ParserUtil.BeanInfo beanInfo1 = ParserUtil.getBeanInfo(TestBean.class);
        
        // Refresh the cache
        ParserUtil.refreshBeanPropInfo(TestBean.class);
        
        // Get bean info again - should be a new instance
        ParserUtil.BeanInfo beanInfo2 = ParserUtil.getBeanInfo(TestBean.class);
        
        // Due to the refresh, these should be different instances
        Assertions.assertNotSame(beanInfo1, beanInfo2);
    }
    
    // Test classes for testing
    @JsonXmlConfig(namingPolicy = NamingPolicy.LOWER_CAMEL_CASE)
    public static class TestBean {
        @Id
        private Long id;
        
        private String simpleName;
        
        @JsonXmlField(name = "customJsonName", dateFormat = "yyyy-MM-dd")
        private Date jsonFieldName;
        
        @Column("db_column")
        private String columnName;
        
        @Transient
        private String transientField;
        
        @JsonXmlField(alias = {"alias1", "alias2"})
        private String aliasedField;
        
        @JsonXmlField(numberFormat = "#,##0.00")
        private Double numberField;
        
        @JsonXmlField(enumerated = EnumBy.ORDINAL)
        private TestEnum enumField;
        
        @ReadOnlyId
        private String readOnlyId;
        
        @JsonXmlField(isJsonRawValue = true)
        private String jsonRawField;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getSimpleName() { return simpleName; }
        public void setSimpleName(String simpleName) { this.simpleName = simpleName; }
        
        public Date getJsonFieldName() { return jsonFieldName; }
        public void setJsonFieldName(Date jsonFieldName) { this.jsonFieldName = jsonFieldName; }
        
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        
        public String getTransientField() { return transientField; }
        public void setTransientField(String transientField) { this.transientField = transientField; }
        
        public String getAliasedField() { return aliasedField; }
        public void setAliasedField(String aliasedField) { this.aliasedField = aliasedField; }
        
        public Double getNumberField() { return numberField; }
        public void setNumberField(Double numberField) { this.numberField = numberField; }
        
        public TestEnum getEnumField() { return enumField; }
        public void setEnumField(TestEnum enumField) { this.enumField = enumField; }
        
        public String getReadOnlyId() { return readOnlyId; }
        public void setReadOnlyId(String readOnlyId) { this.readOnlyId = readOnlyId; }
        
        public String getJsonRawField() { return jsonRawField; }
        public void setJsonRawField(String jsonRawField) { this.jsonRawField = jsonRawField; }
    }
    
    public enum TestEnum {
        VALUE1, VALUE2
    }
    
    // Empty class with no properties for testing
    public static class EmptyClass {
        // No fields or methods
    }
}
