package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.DateTimeFormat;

public class ConfigTest extends AbstractTest {

    @Test
    public void test_1() {
        assertDoesNotThrow(() -> {
            JsonSerConfig.create();
            JsonSerConfig.create().setDateTimeFormat(DateTimeFormat.LONG);
            JsonSerConfig.create().setQuotePropName(true).setQuoteMapKey(true);
            JsonSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames((Map<Class<?>, Set<String>>) null);
            JsonSerConfig.create()
                    .setQuotePropName(true)
                    .setQuoteMapKey(true)
                    .setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP)
                    .setExclusion(Exclusion.DEFAULT)
                    .setIgnoredPropNames((Map<Class<?>, Set<String>>) null);

            XmlSerConfig.create();
            XmlSerConfig.create().setDateTimeFormat(DateTimeFormat.LONG);
            XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(true);
            XmlSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames((Map<Class<?>, Set<String>>) null);

            KryoSerConfig.create();
            KryoSerConfig.create().setExclusion(Exclusion.NULL).setIgnoredPropNames((Map<Class<?>, Set<String>>) null);
        });
    }

}
