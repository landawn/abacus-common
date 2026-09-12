package com.landawn.abacus.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Tests for {@link SecurityUtil}.
 *
 * <p>{@code SecurityUtil} is currently an empty compatibility shell.
 * These tests pin its structural contract so future additions don't accidentally
 * change the class shape (final, non-instantiable, no public state).</p>
 */
public class SecurityUtilTest extends TestBase {

    @Test
    public void testClassIsFinal() {
        Assertions.assertTrue(Modifier.isFinal(SecurityUtil.class.getModifiers()), "SecurityUtil should be declared final");
    }

    @Test
    public void testClassIsPublic() {
        Assertions.assertTrue(Modifier.isPublic(SecurityUtil.class.getModifiers()), "SecurityUtil should be public");
    }

    @Test
    public void testNoPublicConstructors() {
        Constructor<?>[] ctors = SecurityUtil.class.getDeclaredConstructors();
        for (Constructor<?> c : ctors) {
            Assertions.assertFalse(Modifier.isPublic(c.getModifiers()), "SecurityUtil should not expose any public constructor");
        }
    }

    @Test
    public void testPrivateConstructorIsInvocableViaReflection() throws Exception {
        Constructor<?> c = SecurityUtil.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(c.getModifiers()));
        c.setAccessible(true);
        Object instance = c.newInstance();
        Assertions.assertNotNull(instance);
    }
}
