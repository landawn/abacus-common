package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class BeansPublicFieldReview20261003Test extends TestBase {
    @Test
    void publicMutableFieldSurvivesAddingAGetter() {
        assertEquals(List.of("name"), Beans.getPropNameList(FieldWithGetter.class));
        assertNotNull(Beans.getPropGetter(FieldWithGetter.class, "name"));
        assertNull(Beans.getPropSetter(FieldWithGetter.class, "name"));
        assertEquals(Map.of("name", "initial"), Beans.beanToMap(new FieldWithGetter()));
        assertEquals("updated", Beans.mapToBean(Map.of("name", "updated"), FieldWithGetter.class).name);
    }

    @Test
    void inheritedAndNonPublicDeclaringClassesRemainWritable() {
        assertEquals("child", Beans.mapToBean(Map.of("name", "child"), Child.class).name);
        assertEquals("hidden", Beans.mapToBean(Map.of("name", "hidden"), Hidden.class).name);
        assertFalse(Beans.getBeanInfo(Hidden.class).isImmutable);
    }

    @Test
    void ordinaryFieldAndAccessorExclusionsRemainUnchanged() {
        assertEquals(List.of("name"), Beans.getPropNameList(BareField.class));
        assertEquals(List.of("included"), Beans.getPropNameList(ExcludedFields.class));
        assertEquals("ok", Beans.mapToBean(Map.of("included", "ok"), ExcludedFields.class).included);
    }

    public static class FieldWithGetter {
        public String name = "initial";

        public String getName() {
            return name;
        }
    }

    public static class Child extends FieldWithGetter {
    }

    static class Hidden {
        public String name;

        public String getName() {
            return name;
        }
    }

    public static class BareField {
        public String name;
    }

    public static class ExcludedFields {
        public String included;
        private String privateValue;
        public static String staticValue;
        public final String finalValue = "constant";

        public String getPrivateValue() {
            return privateValue;
        }

        public String getStaticValue() {
            return staticValue;
        }

        public String getFinalValue() {
            return finalValue;
        }
    }
}
