package com.landawn.abacus.eventbus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class EventBusGenericTest extends TestBase {
    public static class Overloaded implements Subscriber<String> {
        int strings;
        int integers;

        @Override
        public void on(String event) {
            strings++;
        }

        public void on(Integer event) {
            integers++;
        }
    }

    public interface GenericHandler<T> {
        @Subscribe(eventId = "parent", sticky = true)
        void handle(T event);
    }

    public static class ConcreteHandler implements GenericHandler<String> {
        final List<String> values = new ArrayList<>();

        @Override
        @Subscribe(eventId = "child", sticky = true)
        public void handle(String event) {
            values.add(event);
        }
    }

    public static class InheritedAnnotation implements GenericHandler<String> {
        final List<String> values = new ArrayList<>();

        @Override
        public void handle(String event) {
            values.add(event);
        }
    }

    public static class GenericBase<T> implements Subscriber<T> {
        final List<T> values = new ArrayList<>();

        @Override
        public void on(T event) {
            values.add(event);
        }
    }

    public static class Middle<T> extends GenericBase<T> {
    }

    public static class Strings extends Middle<String> {
    }

    public static class ArrayBase<T> implements Subscriber<T[]> {
        final List<T[]> values = new ArrayList<>();

        @Override
        public void on(T[] event) {
            values.add(event);
        }
    }

    public static class StringArrays extends ArrayBase<String> {
    }

    public static class Bounded<T extends Number> implements Subscriber<T> {
        final List<T> values = new ArrayList<>();

        @Override
        public void on(T event) {
            values.add(event);
        }
    }

    public static class PrimitiveOverloads {
        int primitives;
        int boxes;

        @Subscribe
        public void handle(int value) {
            primitives++;
        }

        @Subscribe
        public void handle(Integer value) {
            boxes++;
        }
    }

    public interface ConfiguredInterface {
        @Subscribe(eventId = "interface")
        void handle(String event);
    }

    public static class ConfiguredBase {
        final List<String> values = new ArrayList<>();

        @Subscribe(eventId = "class")
        public void handle(String event) {
            values.add(event);
        }
    }

    public static class ClassWins extends ConfiguredBase implements ConfiguredInterface {
    }

    public static class AnnotatedBase<T> implements Subscriber<T> {
        final List<T> values = new ArrayList<>();

        @Override
        @Subscribe(eventId = "inherited", strictEventType = true)
        public void on(T event) {
            values.add(event);
        }
    }

    public static class AnnotatedStrings extends AnnotatedBase<CharSequence> {
    }

    public interface OtherConfiguredInterface {
        @Subscribe(eventId = "other")
        void handle(String event);
    }

    public static class Conflicting implements ConfiguredInterface, OtherConfiguredInterface {
        @Override
        public void handle(String event) {
        }
    }

    public static class ResolvedConflict implements ConfiguredInterface, OtherConfiguredInterface {
        int calls;

        @Override
        @Subscribe(eventId = "resolved")
        public void handle(String event) {
            calls++;
        }
    }

    public static class Owner<T> {
        public class Handler implements Subscriber<T> {
            final List<T> values = new ArrayList<>();

            @Override
            public void on(T event) {
                values.add(event);
            }
        }
    }

    public static class SwappingOwner<A, B> {
        public class Base<T> implements Subscriber<T> {
            final List<T> values = new ArrayList<>();

            @Override
            public void on(T event) {
                values.add(event);
            }
        }

        public class Swapped extends SwappingOwner<B, A>.Base<A> {
            public Swapped(final SwappingOwner<B, A> owner) {
                owner.super();
            }
        }
    }

    @Test
    public void onlyActualSubscriberContractIsImplicitlyRegistered() {
        final EventBus bus = EventBus.create();
        final Overloaded handler = new Overloaded();
        bus.register(handler, "events");
        bus.post("events", "");
        bus.post("events", "\u03bb\ud83d\ude00");
        bus.post("events", Integer.MAX_VALUE);
        assertEquals(2, handler.strings);
        assertEquals(0, handler.integers);
        assertFalse(bus.hasSubscribers(Integer.class));
    }

    @Test
    public void annotatedGenericOverrideDispatchesOnceWithChildConfiguration() {
        final EventBus bus = EventBus.create();
        final ConcreteHandler handler = new ConcreteHandler();
        bus.postSticky("parent", "wrong");
        bus.postSticky("child", "retained");
        bus.register(handler);
        bus.post("parent", "wrong");
        bus.post("child", "live");
        assertEquals(List.of("retained", "live"), handler.values);
        assertFalse(bus.hasSubscribers(Integer.class));
        assertTrue(bus.subscribers("parent", String.class).isEmpty());
    }

    @Test
    public void unannotatedOverrideInheritsGenericAnnotationAndMatchingType() {
        final EventBus bus = EventBus.create();
        final InheritedAnnotation handler = new InheritedAnnotation();
        bus.register(handler);
        bus.post("parent", "\u03bb");
        assertEquals(List.of("\u03bb"), handler.values);
        assertFalse(bus.hasSubscribers(Integer.class));
    }

    @Test
    public void multiLevelGenericSubscriberDoesNotNeedLambdaEventId() {
        final EventBus bus = EventBus.create();
        final Strings handler = new Strings();
        bus.register(handler);
        bus.post("");
        bus.post("\u03bb\ud83d\ude00");
        bus.post(1);
        assertEquals(List.of("", "\u03bb\ud83d\ude00"), handler.values);
        assertFalse(bus.hasSubscribers(Integer.class));
        assertEquals(List.of(handler), bus.subscribers(String.class));
        bus.unregister(handler);
        assertFalse(bus.hasSubscribers(String.class));
        assertThrows(IllegalArgumentException.class, () -> bus.post(null));
    }

    @Test
    public void genericArrayUsesResolvedComponentType() {
        final EventBus bus = EventBus.create();
        final StringArrays handler = new StringArrays();
        final String[] empty = new String[0];
        final String[] values = { "\u03bb", null };
        bus.register(handler);
        bus.post(empty);
        bus.post(values);
        bus.post(new Integer[] { 1 });
        assertEquals(2, handler.values.size());
        assertSame(empty, handler.values.get(0));
        assertSame(values, handler.values.get(1));
        assertFalse(bus.hasSubscribers(Integer[].class));
    }

    @Test
    public void unresolvedGenericBoundStillFiltersEvents() {
        final EventBus bus = EventBus.create();
        final Bounded<Number> handler = new Bounded<>();
        bus.register(handler);
        bus.post(1);
        bus.post(Double.NaN);
        bus.post("wrong");
        assertEquals(List.of(1, Double.NaN), handler.values);
    }

    @Test
    public void primitiveAndBoxedAnnotatedOverloadsRemainDistinct() {
        final EventBus bus = EventBus.create();
        final PrimitiveOverloads handler = new PrimitiveOverloads();
        bus.register(handler);
        bus.post(Integer.MIN_VALUE);
        assertEquals(1, handler.primitives);
        assertEquals(1, handler.boxes);
    }

    @Test
    public void classAnnotationWinsOverUnrelatedInterfaceDeclaration() {
        final EventBus bus = EventBus.create();
        final ClassWins handler = new ClassWins();
        bus.register(handler);
        bus.post("class", "correct");
        bus.post("interface", "wrong");
        assertEquals(List.of("correct"), handler.values);
    }

    @Test
    public void inheritedAnnotatedSubscriberKeepsStrictGenericType() {
        final EventBus bus = EventBus.create();
        final AnnotatedStrings handler = new AnnotatedStrings();
        bus.register(handler);
        assertFalse(bus.hasSubscribers(String.class));
        assertTrue(bus.hasSubscribers(CharSequence.class));
        assertEquals(List.of(handler), bus.subscribers("inherited", CharSequence.class));
    }

    @Test
    public void lambdaStillRequiresAnIdAndUsesRegistrationOverride() {
        final EventBus bus = EventBus.create();
        final List<String> values = new ArrayList<>();
        final Subscriber<String> handler = values::add;
        assertThrows(IllegalStateException.class, () -> bus.register(handler));
        assertThrows(IllegalStateException.class, () -> bus.register(handler, ""));
        bus.register(handler, "\u03bb");
        bus.post("\u03bb", "value");
        assertEquals(List.of("value"), values);
    }

    @Test
    public void incompatibleInterfaceAnnotationsFailBeforeRegistration() {
        final EventBus bus = EventBus.create();
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> bus.register(new Conflicting()));
        assertTrue(error.getMessage().contains("Conflicting @Subscribe"));
        assertEquals(0, bus.countOfSubscribers());
        final ResolvedConflict handler = new ResolvedConflict();
        bus.register(handler);
        bus.post("resolved", "");
        bus.post("other", "wrong");
        assertEquals(1, handler.calls);
    }

    @Test
    public void genericOwnerSuppliesNestedSubscriberType() {
        final EventBus bus = EventBus.create();
        final Owner<String>.Handler handler = new Owner<String>().new Handler() {
        };
        bus.register(handler);
        bus.post("\u03bb");
        bus.post(1);
        assertEquals(List.of("\u03bb"), handler.values);
        assertFalse(bus.hasSubscribers(Integer.class));
    }

    @Test
    public void ownerRebindingDoesNotChangeLexicalMethodTypeArguments() {
        final EventBus bus = EventBus.create();
        final SwappingOwner<String, Integer>.Swapped handler = new SwappingOwner<String, Integer>().new Swapped(new SwappingOwner<>()) {
        };
        bus.register(handler);
        bus.post("\u03bb");
        bus.post(1);
        assertEquals(List.of("\u03bb"), handler.values);
    }
}
