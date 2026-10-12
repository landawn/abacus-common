package testfixtures;

import java.util.concurrent.CopyOnWriteArrayList;

/** Outside the library's built-in namespace so the supplier-registration contract can be exercised. */
public final class XmlPerformanceMemoryFixtures {
    private XmlPerformanceMemoryFixtures() {
    }

    public static class FactoryCow extends CopyOnWriteArrayList<Object> {
        public final String marker;

        public FactoryCow(final String marker) {
            this.marker = marker;
        }
    }
}
