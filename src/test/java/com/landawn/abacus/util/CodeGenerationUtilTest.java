package com.landawn.abacus.util;

import java.util.Collection;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.entity.pjo.basic.Account;
import com.landawn.abacus.util.CodeGenerationUtil.PropNameTableCodeConfig;

public class CodeGenerationUtilTest {

    @Test
    public void test_generatePropNameTableClasses() {
        N.println(CodeGenerationUtil.generatePropNameTableClass(Account.class, CodeGenerationUtil.X, "./src/test/resources"));

        final Collection<Class<?>> classes = N.concat(ClassUtil.findClassesInPackage(Account.class.getPackageName(), false, false));

        final PropNameTableCodeConfig codeConfig = PropNameTableCodeConfig.builder()
                .entityClasses(classes)
                .className(CodeGenerationUtil.S)
                .packageName("com.landawn.abacus.entity.pjo.basic")
                .srcDir("./src/test/resources")
                .propNameConverter((cls, propName) -> propName.equals("create_time") ? "createdTime" : propName)
                .generateClassPropNameList(true)
                .generateSnakeCase(true)
                .generateScreamingSnakeCase(true)
                .classNameForScreamingSnakeCase("sau")
                .generateFunctionPropName(true)
                .functionClassName("f")
                .propFunctions(CommonUtil.asLinkedHashMap("min", CodeGenerationUtil.MIN_FUNC, "max", CodeGenerationUtil.MAX_FUNC))
                .build();

        N.println(CodeGenerationUtil.generatePropNameTableClasses(codeConfig));
    }

}
