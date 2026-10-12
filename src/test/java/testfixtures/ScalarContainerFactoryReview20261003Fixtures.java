package testfixtures;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public final class ScalarContainerFactoryReview20261003Fixtures {
    public static class PrefilledList extends ArrayList<Object> {
        public PrefilledList() {
            add("foreign");
        }
    }

    public static class SuppliedList extends ArrayList<Object> {
    }

    public static class ConstrainedList<E> extends ArrayList<E> {
    }

    public static class AliasList extends ArrayList<Object> {
    }

    public static class ReusedList extends ArrayList<Object> {
    }

    public static class FreshList extends ArrayList<Object> {
    }

    public static class PrefilledMap extends LinkedHashMap<String, Object> {
        public PrefilledMap() {
            put("foreign", 9);
        }
    }

    public static class SuppliedMap extends LinkedHashMap<String, Object> {
    }

    public static class FreshMap extends LinkedHashMap<String, Object> {
    }

    public static class FallbackMap extends LinkedHashMap<String, Object> {
    }

    public static class Bean {
        public int value = 1;
    }

    public static class RetryBean {
        private int calls;

        public int getValue() {
            if (++calls == 1) {
                throw new IllegalStateException("first read fails");
            }
            return 1;
        }

        public void setValue(int value) {
        }

        public int calls() {
            return calls;
        }
    }
}
