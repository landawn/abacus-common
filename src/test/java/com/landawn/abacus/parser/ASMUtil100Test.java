package com.landawn.abacus.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.entity.XBean;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.Profiler;

@Tag("new-test")
public class ASMUtil100Test extends TestBase {

    @Test
    public void testIsASMAvailable() {
        boolean result = ASMUtil.isASMAvailable();
        Assertions.assertEquals(result, ASMUtil.isASMAvailable());
        Assertions.assertEquals(result, ASMUtil.isASMAvailable());
    }

    @Test
    public void testIsASMAvailableConsistency() {
        boolean firstCall = ASMUtil.isASMAvailable();
        boolean secondCall = ASMUtil.isASMAvailable();
        boolean thirdCall = ASMUtil.isASMAvailable();

        Assertions.assertEquals(firstCall, secondCall);
        Assertions.assertEquals(secondCall, thirdCall);
    }

    @Test
    public void test_performance() {
        XBean xbean = Beans.newRandom(XBean.class);

        {
            BeanInfo beanInfo = ParserUtil.getBeanInfo(XBean.class, false);

            Profiler.run(1, 100000, 3, "jdk", () -> {

                for (PropInfo propInfo : beanInfo.propInfoList) {
                    propInfo.getPropValue(xbean);
                }
            }).printResult();
        }

        {
            BeanInfo beanInfo = ParserUtil.getBeanInfo(XBean.class, true);

            Profiler.run(1, 100000, 3, "asm", () -> {

                for (PropInfo propInfo : beanInfo.propInfoList) {
                    propInfo.getPropValue(xbean);
                }
            }).printResult();
        }

    }
}
