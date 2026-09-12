package com.landawn.abacus.util;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Record;

public abstract class BeansTestSupport extends TestBase {

    public static class SimpleBean {
        protected String name;
        protected int age;
        protected Boolean active;

        public SimpleBean() {
        }

        public SimpleBean(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public Boolean getActive() {
            return active;
        }

        public void setActive(Boolean active) {
            this.active = active;
        }
    }

    public static class NestedBean {
        protected String id;
        protected SimpleBean simpleBean;
        protected Address address;
        protected List<String> tags;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public SimpleBean getSimpleBean() {
            return simpleBean;
        }

        public void setSimpleBean(SimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }
    }

    public static class Address {
        protected String street;
        protected String city;
        protected String zipCode;

        public Address() {
        }

        public Address(String city) {
            this.city = city;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }
    }

    public static class NoDefaultConstructorBean {
        protected final String name;

        public NoDefaultConstructorBean(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    @Entity
    public static class EntityBean {
        protected Long id;
        protected String value;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Record
    public static class RecordBean {
        protected String recordId;

        public String getRecordId() {
            return recordId;
        }

        public void setRecordId(String recordId) {
            this.recordId = recordId;
        }
    }

    public static class BeanWithDiffIgnore {
        protected String name;
        @DiffIgnore
        protected Date lastModified;
        @DiffIgnore
        protected String internalFlag;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public String getInternalFlag() {
            return internalFlag;
        }

        public void setInternalFlag(String internalFlag) {
            this.internalFlag = internalFlag;
        }
    }

    public static class BeanWithBuilder {
        protected String value;
        protected int number;

        protected BeanWithBuilder() {
        }

        public String getValue() {
            return value;
        }

        public int getNumber() {
            return number;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            protected String value;
            protected int number;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public Builder number(int number) {
                this.number = number;
                return this;
            }

            public BeanWithBuilder build() {
                BeanWithBuilder bean = new BeanWithBuilder();
                bean.value = this.value;
                bean.number = this.number;
                return bean;
            }
        }
    }

    public static class BeanWithStaticAccessors {
        protected String name;
        protected static String version;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public static String getVersion() {
            return version;
        }

        public static void setVersion(String version) {
            BeanWithStaticAccessors.version = version;
        }
    }

    public static class NonBean {
        protected String field;
    }

    public static class BeanWithSnakeCase {
        protected String firstName;
        protected String lastName;
        protected String userID;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }
    }

    public static class CollectionBean {
        protected List<String> items;

        public List<String> getItems() {
            return items;
        }

        public void setItems(List<String> items) {
            this.items = items;
        }
    }

    public static class MapBean {
        protected Map<String, Integer> values;

        public Map<String, Integer> getValues() {
            return values;
        }

        public void setValues(final Map<String, Integer> values) {
            this.values = values;
        }
    }

    public static class AccessorCacheBean {
        protected String visible;
        protected String hidden;

        public String getVisible() {
            return visible;
        }

        public void setVisible(final String visible) {
            this.visible = visible;
        }

        public String getHidden() {
            return hidden;
        }

        public void setHidden(final String hidden) {
            this.hidden = hidden;
        }
    }

    public static class CustomAccessorBean {
        protected String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class PropertyAccessorBaseBean {
        protected String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class CachedPropertyAccessorSubclass extends PropertyAccessorBaseBean {
        // Dedicated regression type: its metadata is populated before base accessor registration.
    }

    public static class LatePropertyAccessorSubclass extends PropertyAccessorBaseBean {
        // Dedicated regression type: its metadata is first populated after base accessor registration.
    }

    public static class PropertyAccessorOverrideBaseBean {
        protected String baseValue;

        public String getBaseValue() {
            return baseValue;
        }

        public void setBaseValue(final String baseValue) {
            this.baseValue = baseValue;
        }
    }

    public static class BaseFirstPropertyAccessorSubclass extends PropertyAccessorOverrideBaseBean {
        protected String childValue;

        public String getChildValue() {
            return childValue;
        }

        public void setChildValue(final String childValue) {
            this.childValue = childValue;
        }
    }

    public static class SubclassFirstPropertyAccessorSubclass extends PropertyAccessorOverrideBaseBean {
        protected String childValue;

        public String getChildValue() {
            return childValue;
        }

        public void setChildValue(final String childValue) {
            this.childValue = childValue;
        }
    }

    public interface LeftAmbiguousPropertyAccessor {
        String getLeftValue();

        public void setLeftValue(String value);
    }

    public interface RightAmbiguousPropertyAccessor {
        String getRightValue();

        public void setRightValue(String value);
    }

    public static class AmbiguousPropertyAccessorBean implements LeftAmbiguousPropertyAccessor, RightAmbiguousPropertyAccessor {
        protected String leftValue;
        protected String rightValue;

        @Override
        public String getLeftValue() {
            return leftValue;
        }

        @Override
        public void setLeftValue(final String value) {
            leftValue = value;
        }

        @Override
        public String getRightValue() {
            return rightValue;
        }

        @Override
        public void setRightValue(final String value) {
            rightValue = value;
        }
    }

    protected SimpleBean simpleBean;
    protected NestedBean nestedBean;
    protected EntityBean entityBean;

    @BeforeEach
    public void setUp() {
        simpleBean = new SimpleBean("John", 25);
        simpleBean.setActive(true);

        nestedBean = new NestedBean();
        nestedBean.setId("123");
        nestedBean.setSimpleBean(simpleBean);

        Address address = new Address("New York");
        address.setStreet("5th Avenue");
        address.setZipCode("10001");
        nestedBean.setAddress(address);

        entityBean = new EntityBean();
        entityBean.setId(1L);
        entityBean.setValue("test");
    }

    public static class HasPrefixBean {
        protected boolean active;

        public boolean hasActive() {
            return active;
        }
    }

    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @java.lang.annotation.Target({ java.lang.annotation.ElementType.FIELD, java.lang.annotation.ElementType.METHOD })
    public @interface DifferenceIgnore {
    }

    public static class DiffProbeBean {
        protected String name;
        @DifferenceIgnore
        protected String secret;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(final String secret) {
            this.secret = secret;
        }
    }

    public static class XmlRegistryProbeBean {
        protected String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class ConverterProbeBean {
        protected String name;
        protected String nickName;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(final String nickName) {
            this.nickName = nickName;
        }
    }

    public static class BeansBuilderFixture {
        protected final String a;
        protected final int b;

        protected BeansBuilderFixture(final String a, final int b) {
            this.a = a;
            this.b = b;
        }

        public String getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            protected String a;
            protected int b;

            public Builder setA(final String a) {
                this.a = a;
                return this;
            }

            public Builder setB(final int b) {
                this.b = b;
                return this;
            }

            public BeansBuilderFixture build() {
                return new BeansBuilderFixture(a, b);
            }
        }
    }

    public static class BeansMutableFixture {
        protected String a;
        protected int b;

        public String getA() {
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(final int b) {
            this.b = b;
        }
    }

    public static class BeansAccessorAFixture {
        protected String value;
        protected String other;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getOther() {
            return other;
        }

        public void setOther(final String other) {
            this.other = other;
        }
    }

    public static class BeansAccessorBFixture {
        protected String value;
        protected String other;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }

        public String getOther() {
            return other;
        }

        public void setOther(final String other) {
            this.other = other;
        }
    }

    public static class BeansAddrFixture {
        protected String city;

        public String getCity() {
            return city;
        }

        public void setCity(final String city) {
            this.city = city;
        }
    }

    public static class BeansUserFixture {
        protected String name;
        protected BeansAddrFixture address;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public BeansAddrFixture getAddress() {
            return address;
        }

        public void setAddress(final BeansAddrFixture address) {
            this.address = address;
        }
    }

    @Entity
    public static class BeansOrderBaseFixture {
        protected String a;

        public String getA() {
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public String getBaseComputed() { // getter only, no backing field: @Entity makes it a property
            return "b";
        }
    }

    @Entity
    public static class BeansOrderSubFixture extends BeansOrderBaseFixture {
        protected String c;

        public String getC() {
            return c;
        }

        public void setC(final String c) {
            this.c = c;
        }

        public String getSubComputed() {
            return "s";
        }
    }

    public static class BeansNonPropFixture {
        protected String name;
        protected String secret = "init"; // no accessor at all => not a property

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        String peekSecret() {
            return secret;
        }

        void pokeSecret(final String v) {
            secret = v;
        }
    }
}
