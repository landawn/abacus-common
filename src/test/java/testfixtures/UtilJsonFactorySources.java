package testfixtures;

import java.util.ArrayList;
import java.util.HashMap;

public final class UtilJsonFactorySources {
    private UtilJsonFactorySources() {
    }

    public static class SourceList<E> extends ArrayList<E> {
    }

    public static class ReusedList<E> extends ArrayList<E> {
    }

    public static class FilledMap<K, V> extends HashMap<K, V> {
    }
}
