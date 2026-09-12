package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ClassUtilCoverageTest extends TestBase {

    @Test
    public void testGetClassLocation() {
        String location = ClassUtil.getClassLocation(String.class);
        String thisLocation = ClassUtil.getClassLocation(ClassUtilCoverageTest.class);
        assertNotNull(thisLocation);
        assertFalse(thisLocation.isEmpty());
        if (location != null) {
            assertFalse(location.isEmpty());
        }
    }

    @Test
    public void testDecodeUrlPath() throws Exception {
        Method decode = ClassUtil.class.getDeclaredMethod("decodeUrlPath", String.class);
        decode.setAccessible(true);
        assertNull(decode.invoke(null, new Object[] { null }));
        assertEquals("/plain/path", decode.invoke(null, "/plain/path"));
        assertEquals("/a b/c", decode.invoke(null, "/a%20b/c"));
        assertEquals("/a+b", decode.invoke(null, "/a%2Bb"));
        assertEquals("/hash#x", decode.invoke(null, "/hash%23x"));
        assertEquals("/bad%ZZ", decode.invoke(null, "/bad%ZZ"));
        assertEquals("/end%", decode.invoke(null, "/end%"));
        assertEquals("/end%2", decode.invoke(null, "/end%2"));
        assertEquals("/caf\u00e9", decode.invoke(null, "/caf%C3%A9"));
        assertEquals("/mix a%GGx", decode.invoke(null, "/mix%20a%GGx"));
        assertEquals("/%", decode.invoke(null, "/%"));
        assertEquals("", decode.invoke(null, ""));

        // G18-66 (2026-09-08): only ASCII hexadecimal digits form a percent-encoded octet. Character.digit(c, 16)
        // also accepts Arabic-Indic and fullwidth digits, so these used to decode to the byte 0x12 (U+0012);
        // they are malformed escapes and must be kept verbatim.
        assertEquals("/x%\u0661\u0662", decode.invoke(null, "/x%\u0661\u0662"));
        assertEquals("/x%\uff11\uff12", decode.invoke(null, "/x%\uff11\uff12"));
        assertEquals("/x%\u06612", decode.invoke(null, "/x%\u06612"));
        assertEquals("/x%1\u0662", decode.invoke(null, "/x%1\u0662"));
        // ... while ASCII hexadecimal digits in either case still decode.
        assertEquals("/x\u0012", decode.invoke(null, "/x%12"));
        assertEquals("/x\u00ab", decode.invoke(null, "/x%c2%AB"));
    }

    @Test
    public void testFindClassesInPackage() {
        List<Class<?>> classes = ClassUtil.findClassesInPackage("com.landawn.abacus.logging", false, true, clazz -> true);
        assertNotNull(classes);
        assertTrue(classes.size() >= 1);
    }
}
