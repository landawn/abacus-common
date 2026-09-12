package testfixtures;

/** Converter registration fixtures must be outside the library's built-in package namespace. */
public final class UtilConverterSources {
    private UtilConverterSources() {
    }

    public static class Source {
        public String value = "\uD83D\uDE00";
    }

    public static class Child extends Source {
    }

    public static class FacadeSource {
        public String value = "facade";
    }
}
