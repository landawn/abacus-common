package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;

public class PackageUtilTest extends AbstractTest {

    @Test
    public void test_type() {
        String pkgName = Type.class.getPackage().getName();
        List<Class<?>> classes = ClassUtil.findClassesInPackage(pkgName, true, true);

        for (Class<?> cls : classes) {
            if (Type.class.isAssignableFrom(cls)) {
                N.println("classes.add(" + cls.getCanonicalName() + ".class);");
            }
        }
        assertNotNull(classes);
    }

    @Test
    public void testGetClassesForPackageWithinJar() {
        final String pkgName = "lombok";
        final List<Class<?>> directClasses = ClassUtil.findClassesInPackage(pkgName, false, true);

        for (Class<?> clazz : directClasses) {
            N.println(clazz.getCanonicalName() + " : " + clazz);
        }

        assertFalse(directClasses.isEmpty());
        assertTrue(directClasses.stream().allMatch(clazz -> pkgName.equals(clazz.getPackageName())));

        final List<Class<?>> recursiveClasses = ClassUtil.findClassesInPackage(pkgName, true, true);

        for (Class<?> clazz : recursiveClasses) {
            N.println(clazz.getCanonicalName() + " : " + clazz);
        }

        assertTrue(recursiveClasses.containsAll(directClasses));
        assertTrue(recursiveClasses.stream().allMatch(clazz -> pkgName.equals(clazz.getPackageName()) || clazz.getPackageName().startsWith(pkgName + ".")));
        assertTrue(recursiveClasses.stream().anyMatch(clazz -> clazz.getPackageName().startsWith(pkgName + ".")));
    }

    @Test
    public void testNoFoundPackage() {
        try {
            ClassUtil.findClassesInPackage("no.package", true, true);
            fail("should throw RuntimeException");
        } catch (IllegalArgumentException e) {
        }

    }
}
