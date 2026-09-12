package testfixtures;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/** Caller-owned types outside the library package, whose built-in types cannot be registered. */
public final class SupplierReviewFixtures {
    private SupplierReviewFixtures() {
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
