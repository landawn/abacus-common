package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.XmlSerConfig;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.N;

public class AbstractTypeVerify20261027G143Test extends TestBase {

    // ---- deep review 2026-10-27 verify G143 begin ----

    /** Public fixture in com.landawn.abacus.*: its simple name "Entry" resolves to java.util.Map.Entry. */
    public static class Entry {
        private String id;

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }
    }

    /** Public fixture: its simple name "Holder" resolves to com.landawn.abacus.util.Holder. Looked up by its binary name first. */
    public static class Holder {
    }

    /** Public fixture: its simple name "Runnable" resolves to java.lang.Runnable. A handler is registered for it by class. */
    public static class Runnable {
    }

    /** Non-public fixture: its simple name "Pair" resolves to com.landawn.abacus.util.Pair. */
    static class Pair {
    }

    /** Public fixture whose simple name resolves to no class. */
    public static class G143Widget {
    }

    static final class RunnableFixtureType extends AbstractType<Runnable> {
        RunnableFixtureType() {
            super(Runnable.class.getCanonicalName());
        }

        @Override
        public Class<Runnable> javaType() {
            return Runnable.class;
        }

        @Override
        public String stringOf(final Runnable x) {
            return x == null ? null : "runnable";
        }

        @Override
        public Runnable valueOf(final String str) {
            return str == null ? null : new Runnable();
        }
    }

    // Registered once per JVM, before anything else looks the fixture class up (only the registration test touches it).
    private static final class Registration {
        static final RunnableFixtureType REGISTERED = new RunnableFixtureType();

        static {
            TypeFactory.registerType(Runnable.class, REGISTERED);
        }
    }

    // A public class of another library in a com.landawn.abacus.* package whose simple name is another class keeps its canonical name,
    // also for arrays and as a type argument, and every name resolves back to the class itself.
    @Test
    public void publicClassWhoseSimpleNameIsAnotherClassKeepsItsCanonicalName() {
        final String canonical = Entry.class.getCanonicalName();
        final Type<?> type = Type.of(Entry.class);
        assertEquals(canonical, type.name());
        assertSame(type, Type.of(type.name()));
        assertEquals(Entry.class, Type.of(Entry.class.getName()).javaType());
        assertEquals(Entry.class, Type.of(Type.of(Entry.class.getName()).name()).javaType());

        assertEquals(canonical + "[]", Type.of(Entry[].class).name());
        assertEquals(Entry[].class, Type.of(Type.of(Entry[].class).name()).javaType());
        assertEquals(canonical + "[][]", Type.of(Entry[][].class).name());
        assertEquals(Entry[][].class, Type.of(Type.of(Entry[][].class).name()).javaType());

        final Type<?> list = Type.ofList(Entry.class);
        assertEquals("List<" + canonical + ">", list.name());
        assertEquals(Entry.class, Type.of(list.name()).elementType().javaType());
        final Type<?> map = Type.ofMap(String.class, Entry.class);
        assertEquals("Map<String, " + canonical + ">", map.name());
        assertEquals(Entry.class, Type.of(map.name()).parameterTypes().get(1).javaType());
    }

    // A binary ("$") spelling looked up before the class: its name is kept as given and still resolves to the class itself; the class's
    // own handler is named by the canonical name.
    @Test
    public void binarySpellingOfSuchAClassResolvesBackToTheClass() {
        final Type<?> byBinaryName = Type.of(Holder.class.getName());
        assertEquals(Holder.class, byBinaryName.javaType());
        assertEquals(Holder.class, Type.of(byBinaryName.name()).javaType());

        final Type<?> byClass = Type.of(Holder.class);
        assertEquals(Holder.class.getCanonicalName(), byClass.name());
        assertSame(byClass, Type.of(byClass.name()));
    }

    // registerType(Class, Type) publishes a handler for such a class under its canonical name, so it does not take over the short name
    // "Runnable", which keeps naming java.lang.Runnable.
    @Test
    public void registeredHandlerForSuchAClassDoesNotShadowTheShortName() {
        final RunnableFixtureType registered = Registration.REGISTERED;
        assertEquals(Runnable.class.getCanonicalName(), registered.name());
        assertSame(registered, Type.of(Runnable.class));
        assertSame(registered, Type.of(Runnable.class.getCanonicalName()));
        assertSame(registered, Type.of(registered.name()));
        assertEquals(java.lang.Runnable.class, Type.of("Runnable").javaType());
    }

    // Unchanged neighbours: a non-public class keeps the short form (r108 rule) and a public class whose simple name resolves to nothing keeps
    // its canonical name.
    @Test
    public void nonPublicAndUnresolvableSimpleNamesAreUnchanged() {
        assertEquals("Pair", Type.of(Pair.class).name());
        assertEquals(com.landawn.abacus.util.Pair.class, Type.of(Type.of(Pair.class).name()).javaType());
        assertEquals(G143Widget.class.getCanonicalName(), Type.of(G143Widget.class).name());
        assertEquals(G143Widget.class, Type.of(Type.of(G143Widget.class).name()).javaType());
    }

    // Why non-public classes keep the r108 rule: an Arrays.asList value written with type information ("ArrayList<Object>") is read back
    // as a java.util.ArrayList by Dataset JSON and by the Abacus XML parser.
    @Test
    public void arraysAsListValueWrittenWithTypeInformationReadsBack() {
        final Dataset dataset = N.newDataset(List.of("list"), List.<List<?>> of(List.of(Arrays.asList(1, 2))));
        final String json = ParserFactory.createJsonParser().serialize(dataset, JsonSerConfig.create().setWriteColumnType(true));
        final Object fromJson = ParserFactory.createJsonParser().deserialize(json, Dataset.class).getColumn("list").get(0);
        assertEquals(java.util.ArrayList.class, fromJson.getClass(), json);
        assertEquals(List.of(1, 2), fromJson, json);

        final Map<String, Object> map = new LinkedHashMap<>();
        map.put("list", Arrays.asList(1, 2));
        final String xml = ParserFactory.createAbacusXmlParser().serialize(map, XmlSerConfig.create().setWriteTypeInfo(true));
        final Object fromXml = ParserFactory.createAbacusXmlParser().deserialize(xml, Map.class).get("list");
        assertEquals(java.util.ArrayList.class, fromXml.getClass(), xml);
        assertEquals(List.of(1, 2), fromXml, xml);
    }

    // ---- deep review 2026-10-27 verify G143 end ----
}
