package performanceintegration;

/** A small package keeps XML class-name discovery in this regression fixture inexpensive. */
public final class PerformanceIntegrationXmlModel {
    public static class Outer {
        private Inner child;
        public Inner getChild() { return child; }
        public void setChild(Inner value) { child = value; }
    }
    public static class Inner {
        private String name;
        public String getName() { return name; }
        public void setName(String value) { name = value; }
    }
}
