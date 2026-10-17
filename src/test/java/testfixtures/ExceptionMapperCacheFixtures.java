package testfixtures;

/** Checked exception types accepted by the public mapper registration API. */
public final class ExceptionMapperCacheFixtures {
    private ExceptionMapperCacheFixtures() {
    }

    public static final class DirectFailure extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static final class RacingFailure extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static final class ReentrantFailure extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static final class VariableFailure extends Exception {
        private static final long serialVersionUID = 1L;

        public VariableFailure(final String message) {
            super(message);
        }
    }

    public static final class StatefulFailure extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static final class FailingMapperFailure extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
