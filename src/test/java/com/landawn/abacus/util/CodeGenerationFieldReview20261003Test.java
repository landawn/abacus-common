package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CodeGenerationUtil.PropNameTableCodeConfig;

public class CodeGenerationFieldReview20261003Test extends TestBase {
    public static class ParentFields {
        public long count;
    }

    public static class PublicFields extends ParentFields {
        public int age;
        public String firstName;
        public Object payload;
    }

    public static class GetterBean {
        private Integer age;

        public Integer getAge() {
            return age;
        }

        public void setAge(final Integer age) {
            this.age = age;
        }
    }

    @Test
    public void minMaxFunctionsIncludePublicAndInheritedFields() {
        final String code = CodeGenerationUtil.generatePropNameTableClasses(new PropNameTableCodeConfig().setEntityClasses(List.of(PublicFields.class))
                .setClassName("Props")
                .setGenerateFunctionPropName(true)
                .setPropFunctions(Map.of("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC)));
        for (final String prefix : List.of("min", "max")) {
            for (final String name : List.of("count", "age", "firstName")) {
                assertTrue(code.contains("String " + prefix + "_" + name + " = \"" + prefix + "(" + name + ")\";"), code);
            }
        }
        assertFalse(code.contains("String min_payload"), code);
        assertFalse(code.contains("String max_payload"), code);
    }

    @Test
    public void callbacksReceiveFieldTypesAndConvertedNames() {
        final Map<String, Class<?>> types = new LinkedHashMap<>();
        final String code = CodeGenerationUtil.generatePropNameTableClasses(new PropNameTableCodeConfig().setEntityClasses(List.of(PublicFields.class))
                .setClassName("Props")
                .setPropNameConverter((bean, name) -> name.equals("age") ? null : name.equals("firstName") ? "displayName" : name)
                .setGenerateFunctionPropName(true)
                .setPropFunctions(Map.of("f", (bean, type, name) -> {
                    assertEquals(PublicFields.class, bean);
                    types.put(name, type);
                    return name.equals("payload") ? "" : "f(" + name + ")";
                })));
        assertEquals(Map.of("count", long.class, "displayName", String.class, "payload", Object.class), types);
        assertTrue(code.contains("String f_displayName = \"f(displayName)\";"), code);
        assertFalse(code.contains("String f_payload"), code);
        assertFalse(code.contains("String f_age"), code);
    }

    @Test
    public void getterPropertiesKeepTheirExistingTypeAndFunctionOutput() {
        final Map<String, Class<?>> types = new LinkedHashMap<>();
        final String code = CodeGenerationUtil.generatePropNameTableClasses(new PropNameTableCodeConfig().setEntityClasses(List.of(GetterBean.class))
                .setClassName("Props")
                .setGenerateFunctionPropName(true)
                .setPropFunctions(Map.of("min", (bean, type, name) -> {
                    types.put(name, type);
                    return CodeGenerationUtil.MIN_FUNC.apply(bean, type, name);
                })));
        assertEquals(Map.of("age", Integer.class), types);
        assertTrue(code.contains("String min_age = \"min(age)\";"), code);
    }
}
