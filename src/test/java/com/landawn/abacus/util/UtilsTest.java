package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for the package-private {@link Utils} registry. We use reflection because the class is
 * package-private and most fields are not exposed publicly.
 */
public class UtilsTest extends TestBase {

    @Test
    public void testJsonParserAlwaysAvailable() throws Exception {
        Field f = Utils.class.getDeclaredField("jsonParser");
        f.setAccessible(true);
        assertNotNull(f.get(null), "jsonParser must always be available");
    }

    @Test
    public void testTypeFieldsArePopulated() throws Exception {
        for (String name : new String[] { "booleanType", "charType", "byteType", "shortType", "intType", "longType", "floatType", "doubleType" }) {
            Field f = Utils.class.getDeclaredField(name);
            f.setAccessible(true);
            assertNotNull(f.get(null), name + " must be initialized");
        }
    }

    @Test
    public void testJsonSerConfigsAreNotNull() throws Exception {
        for (String name : new String[] { "jsc", "jscPrettyFormat" }) {
            Field f = Utils.class.getDeclaredField(name);
            f.setAccessible(true);
            assertNotNull(f.get(null), name + " must not be null");
        }
    }

    @Test
    public void testXmlSerConfigsAreNotNull() throws Exception {
        for (String name : new String[] { "xsc", "xscPrettyFormat", "xscForClone" }) {
            Field f = Utils.class.getDeclaredField(name);
            f.setAccessible(true);
            assertNotNull(f.get(null), name + " must not be null");
        }
    }

    @Test
    public void testClassIsFinalAndPackagePrivate() {
        int mods = Utils.class.getModifiers();
        assertTrue(Modifier.isFinal(mods), "Utils must be final");
        assertEquals(false, Modifier.isPublic(mods), "Utils must not be public");
    }

    @Test
    public void testHasPrivateConstructor() {
        Constructor<?>[] ctors = Utils.class.getDeclaredConstructors();
        assertEquals(1, ctors.length);
        assertTrue(Modifier.isPrivate(ctors[0].getModifiers()));
    }
}
