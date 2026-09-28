package testfixtures;

/**
 * Exception types used only by {@code com.landawn.abacus.util.FnReview20260924bTest}.
 *
 * <p>They live outside {@code com.landawn.abacus} because
 * {@code ExceptionUtil.registerRuntimeExceptionMapper} refuses classes from that package. Mappers are registered for
 * them in a process-wide registry, so no other test may use these classes.</p>
 */
public final class FnReviewMappedExceptions {

    private FnReviewMappedExceptions() {
    }

    /** A runtime exception for which the test registers a mapper. */
    public static class MappedRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public MappedRuntimeException(final String message) {
            super(message);
        }
    }

    /** A subclass of {@link MappedRuntimeException}: no mapper of its own, resolved through its superclass. */
    public static class MappedRuntimeSubException extends MappedRuntimeException {
        private static final long serialVersionUID = 1L;

        public MappedRuntimeSubException(final String message) {
            super(message);
        }
    }

    /** A checked exception for which the test registers a mapper. */
    public static class MappedCheckedException extends Exception {
        private static final long serialVersionUID = 1L;

        public MappedCheckedException(final String message) {
            super(message);
        }
    }
}
