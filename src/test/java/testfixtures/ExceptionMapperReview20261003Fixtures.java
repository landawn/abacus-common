package testfixtures;

/** Exception types outside the library package so public mapper registration accepts them. */
public final class ExceptionMapperReview20261003Fixtures {
    private ExceptionMapperReview20261003Fixtures() {
    }

    public static class InheritedBase extends Exception {
    }

    public static class InheritedChild extends InheritedBase {
    }

    public static class InheritedGrandchild extends InheritedChild {
    }

    public static class OverrideBase extends Exception {
    }

    public static class OverrideChild extends OverrideBase {
    }

    public static class OverrideGrandchild extends OverrideChild {
    }

    public static class LateBase extends Exception {
    }

    public static class LateChild extends LateBase {
    }

    public static class ForceBase extends Exception {
    }

    public static class ForceChild extends ForceBase {
    }

    public static class NullBase extends Exception {
    }

    public static class NullChild extends NullBase {
    }

    public static class ThrowingBase extends Exception {
    }

    public static class ThrowingChild extends ThrowingBase {
    }

    public static class SequenceBase extends Exception {
    }

    public static class SequenceChild extends SequenceBase {
    }

    public static class Unmapped extends Exception {
    }

    public static class MappedFailure extends RuntimeException {
        public MappedFailure(final Throwable cause) {
            super(cause);
        }
    }

    public static class SpecificFailure extends RuntimeException {
        public SpecificFailure(final Throwable cause) {
            super(cause);
        }
    }
}
