package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class EventBusLookupCacheTest extends TestBase {
    private static Map<?, ?> cache(EventBus bus) throws Exception {
        var field = EventBus.class.getDeclaredField("listOfEventIdSubMap");
        field.setAccessible(true);
        return (Map<?, ?>) field.get(bus);
    }

    @Test
    public void absentIdsAreNotRetained() throws Exception {
        EventBus bus = EventBus.create();
        for (int i = 0; i < 10_000; i++) {
            bus.post("\u8def\u7531\ud83d\ude80-" + i, "");
        }
        assertTrue(cache(bus).isEmpty());
    }

    @Test
    public void aPreviouslyAbsentIdCanBeRegisteredReplacedAndRemoved() throws Exception {
        EventBus bus = EventBus.create();
        String id = "\u4e8b\u4ef6\ud83d\ude80";
        bus.post(id, "before registration");
        List<String> first = new ArrayList<>();
        List<String> second = new ArrayList<>();
        Subscriber<String> one = first::add;
        Subscriber<String> two = second::add;
        bus.register(one, id).post(id, "");
        assertEquals(List.of(""), first);
        assertEquals(1, cache(bus).size());
        bus.register(two, id).post(id, "both");
        assertEquals(List.of("", "both"), first);
        assertEquals(List.of("both"), second);
        bus.unregister(one).post(id, "remaining");
        assertEquals(List.of("both", "remaining"), second);
        bus.unregister(two).post(id, "absent again");
        assertTrue(cache(bus).isEmpty());
    }

    public static class Unfiltered {
        final List<String> events = new ArrayList<>();

        @Subscribe
        public void on(String event) {
            events.add(event);
        }
    }

    @Test
    public void nullEmptyAndStickyIdsKeepTheirDeliveryRules() throws Exception {
        EventBus bus = EventBus.create();
        Unfiltered handler = new Unfiltered();
        bus.register(handler);
        bus.post((String) null, "null").post("", "").post("missing", "ignored");
        assertEquals(List.of("null", ""), handler.events);
        assertTrue(cache(bus).isEmpty());
        String id = "\u9ecf\u6027\ud83d\ude80";
        bus.postSticky(id, "retained");
        assertTrue(cache(bus).isEmpty());
        class Sticky {
            final List<String> events = new ArrayList<>();

            @Subscribe(sticky = true)
            public void on(String event) {
                events.add(event);
            }
        }
        Sticky sticky = new Sticky();
        bus.register(sticky, id);
        assertEquals(List.of("retained"), sticky.events);
        bus.post(id, "next");
        assertEquals(List.of("retained", "next"), sticky.events);
        assertTrue(bus.removeAllStickyEvents());
        bus.unregister(sticky);
        assertTrue(cache(bus).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> bus.post(id, null));
    }
}
