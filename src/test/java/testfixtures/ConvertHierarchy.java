package testfixtures;

/**
 * Non-abacus types so {@code N.registerConverter} accepts them (built-in package prefixes
 * include {@code com.landawn.abacus.}).
 */
public final class ConvertHierarchy {

    private ConvertHierarchy() {
    }

    public static class Base {
    }

    public static class Child extends Base {
    }
}
