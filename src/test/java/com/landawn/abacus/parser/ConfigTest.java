package com.landawn.abacus.parser;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.parser.JsonSerializationConfig.JSC;
import com.landawn.abacus.parser.KryoSerializationConfig.KSC;
import com.landawn.abacus.parser.XmlSerializationConfig.XSC;
import com.landawn.abacus.util.DateTimeFormat;

@Tag("old-test")
public class ConfigTest extends AbstractTest {

    @Test
    public void test_1() {
        JSC.create();
        JSC.of(DateTimeFormat.LONG);
        JSC.of(true, true);
        JSC.of(Exclusion.NULL, null);
        JSC.of(true, true, DateTimeFormat.ISO_8601_TIMESTAMP, Exclusion.DEFAULT, null);

        XSC.create();
        XSC.of(DateTimeFormat.LONG);
        XSC.of(true, true);
        XSC.of(Exclusion.NULL, null);

        KSC.create();
        KSC.of(Exclusion.NULL, null);
    }

}
