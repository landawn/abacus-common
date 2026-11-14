package com.landawn.abacus.util;

/** Standalone child-JVM fixture: no enclosing JUnit test or test-base class needs to be loaded. */
public final class ReflectAsmProbe {
    static final String EXPECTED = "PROBE available=false get=default newInstance=ok";

    public static final class Target {
        public String publicField = "default";
    }

    public static void main(final String[] args) {
        final String got = Reflection.on(new Target()).get("publicField");
        final Object created = Reflection.on(Target.class).newInstance().instance();
        System.out.println(
                "PROBE available=" + Reflection.isReflectASMAvailable + " get=" + got + " newInstance=" + (created instanceof Target ? "ok" : "FAILED"));
    }
}
