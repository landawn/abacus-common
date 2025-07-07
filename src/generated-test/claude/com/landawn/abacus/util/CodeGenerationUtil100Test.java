package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CodeGenerationUtil.PropNameTableCodeConfig;
import com.landawn.abacus.util.function.TriFunction;

import codes.entity.Account;
import codes.entity.User;

public class CodeGenerationUtil100Test extends TestBase {

    @TempDir
    Path tempDir;

    // Test entity classes for testing
    public static class TestEntity {
        private String id;
        private String name;
        private Integer age;
        private Date createdTime;
        private boolean active;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
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

        public Date getCreatedTime() {
            return createdTime;
        }

        public void setCreatedTime(Date createdTime) {
            this.createdTime = createdTime;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    public static class AnotherTestEntity {
        private Long id;
        private String description;
        private Double price;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }

    // Test for generatePropNameTableClass(Class<?>) - single parameter
    @Test
    public void testGeneratePropNameTableClass_SingleParam() {
        String result = CodeGenerationUtil.generatePropNameTableClass(TestEntity.class);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("public interface x"));
        Assertions.assertTrue(result.contains("String id = \"id\""));
        Assertions.assertTrue(result.contains("String name = \"name\""));
        Assertions.assertTrue(result.contains("String age = \"age\""));
        Assertions.assertTrue(result.contains("String createdTime = \"createdTime\""));
        Assertions.assertTrue(result.contains("String active = \"active\""));
        Assertions.assertTrue(result.contains("Auto-generated class for property(field) name table"));
    }

    // Test for generatePropNameTableClass(Class<?>, String) - two parameters
    @Test
    public void testGeneratePropNameTableClass_TwoParams() {
        String customClassName = "PropNames";
        String result = CodeGenerationUtil.generatePropNameTableClass(TestEntity.class, customClassName);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("public interface " + customClassName));
        Assertions.assertTrue(result.contains("String id = \"id\""));
        Assertions.assertTrue(result.contains("String name = \"name\""));
        Assertions.assertTrue(result.contains("String age = \"age\""));
        Assertions.assertTrue(result.contains("String createdTime = \"createdTime\""));
        Assertions.assertTrue(result.contains("String active = \"active\""));
    }

    // Test for generatePropNameTableClass(Class<?>, String, String) - three parameters with srcDir
    @Test
    public void testGeneratePropNameTableClass_ThreeParams() {
        N.println(CodeGenerationUtil.generatePropNameTableClass(Account.class, CodeGenerationUtil.X, "./src/generated-test/common/"));

        final Collection<Class<?>> classes = N.concat(ClassUtil.getClassesByPackage(User.class.getPackageName(), false, false));

        final PropNameTableCodeConfig codeConfig = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className(CodeGenerationUtil.S)
                .packageName("codes.entity.samples")
                .srcDir("../src/generated-test/common/")
                .propNameConverter((cls, propName) -> propName.equals("create_time") ? "createTime" : propName)
                .generateClassPropNameList(true)
                .generateLowerCaseWithUnderscore(true)
                .generateUpperCaseWithUnderscore(true)
                .generateFunctionPropName(true)
                .functionClassName("f")
                .propFunctions(N.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
                .build();

        N.println(CodeGenerationUtil.generatePropNameTableClasses(codeConfig));
    }

    // Test for generatePropNameTableClasses(Collection<Class<?>>) - single parameter
    @Test
    public void testGeneratePropNameTableClasses_SingleParam() {
        Collection<Class<?>> classes = Arrays.asList(TestEntity.class, AnotherTestEntity.class);
        String result = CodeGenerationUtil.generatePropNameTableClasses(classes);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("public interface s"));
        Assertions.assertTrue(result.contains("String id = \"id\""));
        Assertions.assertTrue(result.contains("String name = \"name\""));
        Assertions.assertTrue(result.contains("String description = \"description\""));
        Assertions.assertTrue(result.contains("String price = \"price\""));
        Assertions.assertTrue(result.contains("[TestEntity, AnotherTestEntity]"));
    }

    // Test for generatePropNameTableClasses(Collection<Class<?>>, String) - two parameters
    @Test
    public void testGeneratePropNameTableClasses_TwoParams() {
        Collection<Class<?>> classes = Arrays.asList(TestEntity.class, AnotherTestEntity.class);
        String className = "SharedProps";
        String result = CodeGenerationUtil.generatePropNameTableClasses(classes, className);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("public interface " + className));
        Assertions.assertTrue(result.contains("String id = \"id\""));
        Assertions.assertTrue(result.contains("for classes: {@code [AnotherTestEntity, TestEntity]}"));
    }

    // Test for generatePropNameTableClasses(Collection<Class<?>>, String, String, String) - four parameters
    @Test
    public void testGeneratePropNameTableClasses_FourParams() throws IOException {
        Collection<Class<?>> classes = Arrays.asList(TestEntity.class, AnotherTestEntity.class);
        String className = "AllProps";
        String packageName = "com.test.generated";
        String srcDir = tempDir.toString();

        String result = CodeGenerationUtil.generatePropNameTableClasses(classes, className, packageName, srcDir);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("package " + packageName));
        Assertions.assertTrue(result.contains("public interface " + className));

        // Verify file was created
        File generatedFile = new File(srcDir, packageName.replace('.', File.separatorChar) + File.separator + className + ".java");
        Assertions.assertTrue(generatedFile.exists());
    }

    // Test for generatePropNameTableClasses(PropNameTableCodeConfig) - config parameter
    @Test
    public void testGeneratePropNameTableClasses_WithConfig() throws IOException {
        Collection<Class<?>> classes = Arrays.asList(TestEntity.class, AnotherTestEntity.class);

        PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className("ConfigProps")
                .packageName("com.test.config")
                .srcDir(tempDir.toString())
                .propNameConverter((cls, propName) -> propName.equals("createdTime") ? "created_time" : propName)
                .generateClassPropNameList(true)
                .generateLowerCaseWithUnderscore(true)
                .classNameForLowerCaseWithUnderscore("sl_custom")
                .generateUpperCaseWithUnderscore(true)
                .classNameForUpperCaseWithUnderscore("su_custom")
                .generateFunctionPropName(true)
                .functionClassName("func")
                .propFunctions(N.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
                .build();

        String result = CodeGenerationUtil.generatePropNameTableClasses(config);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("package com.test.config"));
        Assertions.assertTrue(result.contains("public interface ConfigProps"));
        Assertions.assertTrue(result.contains("public interface sl_custom"));
        Assertions.assertTrue(result.contains("public interface su_custom"));
        Assertions.assertTrue(result.contains("public interface func"));
        Assertions.assertTrue(result.contains("String created_time = \"created_time\""));
        Assertions.assertTrue(result.contains("List<String>"));
        Assertions.assertTrue(result.contains("testEntityPropNameList"));
        Assertions.assertTrue(result.contains("anotherTestEntityPropNameList"));

        // Verify lower case with underscore
        Assertions.assertTrue(result.contains("created_time = \"created_time\""));

        // Verify upper case with underscore
        Assertions.assertTrue(result.contains("CREATED_TIME"));

        // Verify function properties
        Assertions.assertTrue(result.contains("min_"));
        Assertions.assertTrue(result.contains("max_"));
    }

    // Test for edge cases with null/empty inputs
    @Test
    public void testGeneratePropNameTableClasses_EdgeCases() {
        // Test with empty collection
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(Collections.emptyList());
        });

        // Test with null config
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses((PropNameTableCodeConfig) null);
        });

        // Test config with null entity classes
        PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(null).className("Test").build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });

        // Test config with empty className
        PropNameTableCodeConfig config2 = PropNameTableCodeConfig.builder().entityClasses(Arrays.asList(TestEntity.class)).className("").build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config2);
        });
    }

    // Test for interface filtering
    @Test
    public void testGeneratePropNameTableClasses_InterfaceFiltering() {
        interface TestInterface {
            String getValue();
        }

        Collection<Class<?>> classes = Arrays.asList(TestEntity.class, TestInterface.class);
        String result = CodeGenerationUtil.generatePropNameTableClasses(classes);

        Assertions.assertNotNull(result);
        // Interface should be filtered out
        Assertions.assertFalse(result.contains("TestInterface"));
        Assertions.assertTrue(result.contains("TestEntity"));
    }

    // Test for duplicate simple class names
    @Test
    public void testGeneratePropNameTableClasses_DuplicateSimpleClassNames() {
        // Create another TestEntity in different package to test duplicate handling
        class TestEntity {
            private String field1;

            public String getField1() {
                return field1;
            }

            public void setField1(String field1) {
                this.field1 = field1;
            }
        }

        Collection<Class<?>> classes = Arrays.asList(CodeGenerationUtil100Test.TestEntity.class, TestEntity.class);

        PropNameTableCodeConfig config = PropNameTableCodeConfig.builder().entityClasses(classes).className("Props").generateClassPropNameList(true).build();

        // Should throw exception due to duplicate simple class names when generateClassPropNameList is true
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CodeGenerationUtil.generatePropNameTableClasses(config);
        });
    }

    // Test for keyword property names
    @Test
    public void testGeneratePropNameTableClasses_KeywordPropertyNames() {
        class TestEntityWithKeywords {
            private String class_;
            private String if_;

            public String getClass_() {
                return class_;
            }

            public void setClass_(String class_) {
                this.class_ = class_;
            }

            public String getIf_() {
                return if_;
            }

            public void setIf_(String if_) {
                this.if_ = if_;
            }
        }

        Collection<Class<?>> classes = Arrays.asList(TestEntityWithKeywords.class);
        String result = CodeGenerationUtil.generatePropNameTableClasses(classes);

        Assertions.assertNotNull(result);
        // Keywords should be prefixed with underscore
        Assertions.assertTrue(result.contains("String class_ = \"class_\""));
        Assertions.assertTrue(result.contains("String if_ = \"if_\""));
    }

    // Test for custom prop name converters
    @Test
    public void testGeneratePropNameTableClasses_CustomConverters() {
        BiFunction<Class<?>, String, String> customConverter = (cls, propName) -> {
            if (propName.equals("id")) {
                return null; // Skip id
            }
            return propName.toUpperCase();
        };

        PropNameTableCodeConfig config = PropNameTableCodeConfig.builder()
                .entityClasses(Arrays.asList(TestEntity.class))
                .className("CustomProps")
                .propNameConverter(customConverter)
                .build();

        String result = CodeGenerationUtil.generatePropNameTableClasses(config);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.contains("String id")); // id should be skipped
        Assertions.assertTrue(result.contains("String NAME = \"NAME\""));
        Assertions.assertTrue(result.contains("String AGE = \"AGE\""));
    }

    // Test constants
    @Test
    public void testConstants() {
        Assertions.assertEquals("s", CodeGenerationUtil.S);
        Assertions.assertEquals("sl", CodeGenerationUtil.SL);
        Assertions.assertEquals("su", CodeGenerationUtil.SU);
        Assertions.assertEquals("sf", CodeGenerationUtil.SF);
        Assertions.assertEquals("x", CodeGenerationUtil.X);
    }

    // Test MIN_FUNC
    @Test
    public void testMinFunc() {
        TriFunction<Class<?>, Class<?>, String, String> minFunc = CodeGenerationUtil.MIN_FUNC;

        // Test with Comparable property
        String result = minFunc.apply(TestEntity.class, String.class, "name");
        Assertions.assertEquals("min(name)", result);

        // Test with non-Comparable property
        class NonComparable {
        }
        result = minFunc.apply(TestEntity.class, NonComparable.class, "prop");
        Assertions.assertNull(result);
    }

    // Test MAX_FUNC
    @Test
    public void testMaxFunc() {
        TriFunction<Class<?>, Class<?>, String, String> maxFunc = CodeGenerationUtil.MAX_FUNC;

        // Test with Comparable property
        String result = maxFunc.apply(TestEntity.class, Integer.class, "age");
        Assertions.assertEquals("max(age)", result);

        // Test with non-Comparable property
        class NonComparable {
        }
        result = maxFunc.apply(TestEntity.class, NonComparable.class, "prop");
        Assertions.assertNull(result);
    }
}