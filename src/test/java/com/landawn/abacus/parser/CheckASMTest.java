package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CheckASMTest extends TestBase {
    @Test
    public void checkPropInfoType() {
        assertDoesNotThrow(() -> {
            ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(PropInfoTest.TestBean.class);
            ParserUtil.PropInfo jsonRawProp = beanInfo.getPropInfo("jsonRawField");
            System.out.println("PropInfo type: " + jsonRawProp.getClass().getSimpleName());
            System.out.println("Is ASMPropInfo: " + (jsonRawProp instanceof ParserUtil.ASMPropInfo));
            System.out.println("ASM available: " + com.landawn.abacus.parser.ASMUtil.isASMAvailable());
        });
    }
}
