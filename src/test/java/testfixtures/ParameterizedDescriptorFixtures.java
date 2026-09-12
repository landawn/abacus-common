package testfixtures;

/**
 * Non-abacus types so {@code N.registerConverter} accepts them (built-in package prefixes
 * include {@code com.landawn.abacus.}).
 */
public final class ParameterizedDescriptorFixtures {

    private ParameterizedDescriptorFixtures() {
    }

    /**
     * A bean-shaped generic holder: {@code TypeFactory} resolves {@code Generic<String>} to a
     * {@code BeanType} whose {@code parameterTypes()} is non-empty, while {@code Generic.class}
     * resolves to the same handler with no parameter types at all.
     */
    public static final class Generic<T> {
        private T value;

        public Generic() {
        }

        public Generic(final T value) {
            this.value = value;
        }

        public T getValue() {
            return value;
        }

        public void setValue(final T value) {
            this.value = value;
        }
    }
}
