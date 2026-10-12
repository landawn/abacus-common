package testfixtures;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Caller-owned types outside the library package, whose built-in types cannot be registered. */
public final class SupplierReviewFixtures {
    private SupplierReviewFixtures() {
    }

    // Reserved for AnonymousContainerConstructionTest, which registers global factories for these two classes.
    public static java.util.ArrayList<String> anonymousList() {
        return new java.util.ArrayList<>() {
        };
    }

    public static java.util.HashMap<String, Integer> anonymousMap() {
        return new java.util.HashMap<>() {
        };
    }

    // Reserved for AnonymousContainerParserTest, which registers global factories for these five classes. The anonymous
    // factories return pre-filled containers (a reader using them would mix the entry into the parsed data); the named
    // classes need their factories (a constructor argument, a comparator).
    public static java.util.HashMap<String, Object> prefilledAnonymousMap() {
        return new java.util.HashMap<>() {
            {
                put("initializer", "7");
            }
        };
    }

    public static java.util.ArrayList<String> prefilledAnonymousList() {
        return new java.util.ArrayList<>() {
            {
                add("initializer");
            }
        };
    }

    public static class RegisteredNamedMap extends java.util.HashMap<String, Object> {
        private static final long serialVersionUID = 1L;

        public RegisteredNamedMap(final int ignored) {
        }
    }

    public static class RegisteredSortedMap extends java.util.TreeMap<String, Object> {
        private static final long serialVersionUID = 1L;

        public RegisteredSortedMap() {
        }

        public RegisteredSortedMap(final java.util.Comparator<String> comparator) {
            super(comparator);
        }
    }

    public static class RegisteredNamedList extends java.util.ArrayList<String> {
        private static final long serialVersionUID = 1L;

        public RegisteredNamedList(final int ignored) {
        }
    }

    public interface RegisteredSet extends SortedSet<String> {
    }

    public static class ConcreteRegisteredSet extends TreeSet<String> implements RegisteredSet {
    }

    public interface RegisteredConcurrentMap extends ConcurrentNavigableMap<String, Integer> {
    }

    public static class ConcreteRegisteredMap extends ConcurrentSkipListMap<String, Integer> implements RegisteredConcurrentMap {
    }
}
