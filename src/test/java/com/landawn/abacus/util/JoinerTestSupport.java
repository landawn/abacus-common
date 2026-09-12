package com.landawn.abacus.util;

import com.landawn.abacus.AbstractTest;

public abstract class JoinerTestSupport extends AbstractTest {

    protected static final class Person {
        protected String name;
        protected Integer age;
        protected String city;
        protected Double salary;

        Person() {
        }

        Person(String name, Integer age, String city, Double salary) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(Double salary) {
            this.salary = salary;
        }
    }

    protected static final class TestBean {
        protected String name;
        protected Integer value;
        protected Integer age;
        protected String city;
        protected Double salary;
        protected String nullField;
        protected NestedBean nestedBean;

        TestBean() {
            this.name = "test";
            this.value = 123;
        }

        TestBean(String name, Integer age, String city, Double salary) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(Double salary) {
            this.salary = salary;
        }

        public String getNullField() {
            return nullField;
        }

        public void setNullField(String nullField) {
            this.nullField = nullField;
        }

        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }
    }

    protected static final class NestedBean {
        protected String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    // --- Additional gap-filling tests ---

    // appendAll(array, fromIndex, toIndex) with fromIndex==toIndex returns early (empty range)

    // appendEntries(map, fromIndex, toIndex) with fromIndex==toIndex returns early

    // --- Additional gap-filling tests for uncovered paths ---

    // -------------------------------------------------------------------------
    // Bug-regression tests
    // -------------------------------------------------------------------------

    // --- regression tests for 2026-06-10 deep-review fixes ---

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
