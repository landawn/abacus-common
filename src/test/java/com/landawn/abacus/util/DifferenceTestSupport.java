package com.landawn.abacus.util;

import java.util.LinkedHashMap;
import java.util.Map;

import com.landawn.abacus.AbstractTest;

public abstract class DifferenceTestSupport extends AbstractTest {

    public static class DiffIncludedBean {
        protected String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class DiffIgnoredBean {
        @com.landawn.abacus.annotation.DiffIgnore
        protected String value;

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    // --- Primitive list one-null tests ---

    // --- Additional BeanDifference tests ---

    // --- regression tests for 2026-06-11 deep-review fixes ---

    public static class ArrayPropBean {
        protected String name;
        protected byte[] data;
        protected int[][] matrix;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(final byte[] data) {
            this.data = data;
        }

        public int[][] getMatrix() {
            return matrix;
        }

        public void setMatrix(final int[][] matrix) {
            this.matrix = matrix;
        }
    }

    protected static Map<String, Object> mapWithArrayValues(final int id, final byte[] data) {
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("data", data);
        return map;
    }

    // ------------------------------------------------------------------------------------------------
    // Fixtures for the null-property / predicate / equality regressions below.
    // ------------------------------------------------------------------------------------------------

    public static class NullPropBean {
        protected String name;
        protected String email;
        protected Integer score;

        public NullPropBean() {
        }

        public NullPropBean(final String name, final String email, final Integer score) {
            this.name = name;
            this.email = email;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(final Integer score) {
            this.score = score;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // A present-but-null property is still a property the other side does not have. Comparing a bean
    // against null must report every property, so that a bean whose properties are all null can never
    // be reported as equal to null.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // The value-equivalence predicate decides for every property present on both sides, including when
    // both values are null. It used to be short-circuited for that case and silently overridden.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // equals must require the exact same class. A MapDifference over Map<String, Object> and a
    // BeanDifference hold structurally identical containers and used to compare equal.
    // ------------------------------------------------------------------------------------------------

    // ------------------------------------------------------------------------------------------------
    // The key selection is probed once per entry of both maps, so it must not be a linear scan. Copying
    // a non-Set selection into a HashSet must not change which keys are selected, and an existing Set -
    // including a SortedSet whose comparator defines its own key equality - must be used as given.
    // ------------------------------------------------------------------------------------------------

    //
    // ============================ review fixes 2026-09-06 ============================
    //

    public static class ReviewFixes20260906LeftBean {
        protected String userName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }
    }

    public static class ReviewFixes20260906IgnoredLeftBean {
        @com.landawn.abacus.annotation.DiffIgnore
        protected String userName;

        public String getUserName() {
            return userName;
        }

        public void setUserName(final String userName) {
            this.userName = userName;
        }
    }

    public static class ReviewFixes20260906RightBean {
        protected String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(final String username) {
            this.username = username;
        }
    }

    protected static DiffIncludedBean newDiffIncludedBean(final String value) {
        final DiffIncludedBean bean = new DiffIncludedBean();
        bean.setValue(value);
        return bean;
    }
}
