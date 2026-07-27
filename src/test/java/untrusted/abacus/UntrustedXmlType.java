package untrusted.abacus;

/** Test fixture whose initialization is observable without loading it from the parser test. */
public final class UntrustedXmlType {
    static {
        System.setProperty("com.landawn.abacus.test.untrustedXmlTypeInitialized", "true");
    }

    private UntrustedXmlType() {
    }
}
