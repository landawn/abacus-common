package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DeepReview20261004ReflectionTest extends TestBase {

    public interface ParentConstants {
        String LABEL = "interface";
    }

    public interface ChildConstants extends ParentConstants {
    }

    public static class Implementation implements ChildConstants {
    }

    public static class InheritedImplementation extends Implementation {
    }

    public static class HiddenField extends Implementation {
        private final String LABEL = "instance";
    }

    public interface HiddenConstant extends ChildConstants {
        String LABEL = "child interface";
    }

    @Test
    void readsConstantInheritedBySubinterface() {
        assertEquals("interface", Reflection.on(ChildConstants.class).get("LABEL"));
    }

    @Test
    void readsConstantInheritedByImplementingClass() {
        assertEquals("interface", Reflection.on(Implementation.class).get("LABEL"));
        assertEquals("interface", Reflection.on(new Implementation()).get("LABEL"));
        assertEquals("interface", Reflection.on(new InheritedImplementation()).get("LABEL"));
    }

    @Test
    void preservesNearestDeclaredFieldPrecedence() {
        assertEquals("instance", Reflection.on(new HiddenField()).get("LABEL"));
        assertEquals("child interface", Reflection.on(HiddenConstant.class).get("LABEL"));
    }
}
