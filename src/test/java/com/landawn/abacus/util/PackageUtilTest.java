package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;

@Tag("old-test")
public class PackageUtilTest extends AbstractTest {

    @Test
    public void test_type() {
        String pkgName = Type.class.getPackage().getName();
        List<Class<?>> classes = ClassUtil.getClassesByPackage(pkgName, true, true);

        for (Class<?> cls : classes) {
            if (Type.class.isAssignableFrom(cls)) {
                N.println("classes.add(" + cls.getCanonicalName() + ".class);");
            }
        }
    }

    @Test
    public void testGetClassesForPackageWithinJar() {
        String pkgName = "lombok";
        List<Class<?>> classes = ClassUtil.getClassesByPackage(pkgName, false, true);

        for (Class<?> clazz : classes) {
            N.println(clazz.getCanonicalName() + " : " + clazz);
        }

        assertEquals(41, classes.size());
        pkgName = "lombok";
        classes = ClassUtil.getClassesByPackage(pkgName, true, true);

        for (Class<?> clazz : classes) {
            N.println(clazz.getCanonicalName() + " : " + clazz);
        }

        assertEquals(102, classes.size());
    }

    @Test
    public void testNoFoundPackage() {
        try {
            ClassUtil.getClassesByPackage("no.package", true, true);
            fail("should throw RuntimeException");
        } catch (IllegalArgumentException e) {
        }

    }
}
