package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Sanity tests for the empty {@link Script} compatibility shell.
 *
 * <p>Until the class is implemented, ensure it remains:
 * <ul>
 *   <li>final (cannot be subclassed),</li>
 *   <li>has a private no-arg constructor (cannot be reflectively instantiated by accident),</li>
 *   <li>declares no public API.</li>
 * </ul>
 */
public class ScriptTest extends TestBase {

    @Test
    public void testClassIsFinal() {
        assertTrue(Modifier.isFinal(Script.class.getModifiers()), "Script must be final");
    }

    @Test
    public void testClassHasNoPublicMembers() {
        assertEquals(0, Script.class.getDeclaredMethods().length, "Script should declare no methods");
        assertEquals(0, Script.class.getDeclaredFields().length, "Script should declare no fields");
    }

    @Test
    public void testHasOnlyPrivateConstructor() throws Exception {
        Constructor<?>[] ctors = Script.class.getDeclaredConstructors();
        assertEquals(1, ctors.length);
        assertTrue(Modifier.isPrivate(ctors[0].getModifiers()), "Constructor must be private");
    }

    @Test
    public void testReflectiveInstantiationProducesInstance() throws Exception {
        // Even a placeholder utility class is instantiable via setAccessible(true).
        // This is documented: confirming the constructor runs cleanly.
        Constructor<Script> c = Script.class.getDeclaredConstructor();
        c.setAccessible(true);
        Script s = c.newInstance();
        assertNotNull(s);
    }

    @Test
    public void testCannotInvokeUnknownMethod() {
        // Defensive: Script exposes no scripting methods today. If somebody adds
        // a script-eval method, this test deliberately fails to force a security review.
        assertThrows(NoSuchMethodException.class, () -> Script.class.getDeclaredMethod("eval", String.class));
        assertThrows(NoSuchMethodException.class, () -> Script.class.getDeclaredMethod("execute", String.class));
        assertThrows(NoSuchMethodException.class, () -> Script.class.getDeclaredMethod("run", String.class));
    }

    @Test
    public void testReflectiveInvocationOfUnknownMethodFails() {
        // Sanity: any attempt to access non-existent script-execution APIs is rejected.
        assertThrows(NoSuchMethodException.class, () -> {
            try {
                Script.class.getMethod("eval", String.class);
            } catch (NoSuchMethodException e) {
                throw e;
            }
        });
    }
}
