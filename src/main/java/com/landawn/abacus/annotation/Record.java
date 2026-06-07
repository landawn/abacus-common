/*
 * Copyright (C) 2021 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.annotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a class as a record-like entity, telling abacus introspection to treat it as if it were a
 * Java 14+ {@link java.lang.Record} even when it does not extend {@link java.lang.Record} (for
 * example, a Lombok {@code @Value} class compiled under Java 8 source level).
 *
 * <p><b>How the framework consumes it:</b></p>
 * <ul>
 *   <li>{@code com.landawn.abacus.util.Beans#isRecordClass(Class)} returns {@code true} for any
 *       class annotated with {@code @Record}, in addition to actual {@link java.lang.Record}
 *       subclasses. Many downstream helpers (bean introspection, copy/clone, property access)
 *       branch on this method and apply record-style semantics (constructor-based instantiation,
 *       canonical property order, no setters required).</li>
 *   <li>{@code com.landawn.abacus.parser.AvroParser} and similar serializers also detect this
 *       annotation to drive record-aware (de)serialization.</li>
 * </ul>
 *
 * <p><b>Note:</b> The annotation is itself meta-annotated with {@link Test}, signaling that the
 * marker is experimental and may change. Despite that, it is consumed by production framework
 * code, so it is safe to apply where record-like introspection is needed.</p>
 *
 * <p><b>Implied contract for annotated classes:</b></p>
 * <ul>
 *   <li>Instances are effectively immutable.</li>
 *   <li>All component fields are effectively final and exposed via canonical accessor methods.</li>
 *   <li>Equality and {@code hashCode} are based on the component fields.</li>
 *   <li>A canonical all-args constructor exists (matching declaration order of the components).</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 *
 * <p><b>Lombok-backed record-like class on a pre-Java-14 codebase:</b></p>
 * <pre>{@code
 * @Record
 * @Value          // creates all-args ctor, getters, equals/hashCode/toString from final fields (Lombok).
 * public class PersonRecord {
 *     String name;
 *     int    age;
 *     String email;
 * }
 *
 * Beans.isRecordClass(PersonRecord.class);  // true (because of @Record)
 *
 * // Behaves like:
 * // public record PersonRecord(String name, int age, String email) {}
 * }</pre>
 *
 * <p><b>Manually written record-like class:</b></p>
 * <pre>{@code
 * @Record
 * public final class Point {
 *     private final int x;
 *     private final int y;
 *
 *     public Point(int x, int y) { this.x = x; this.y = y; }
 *
 *     public int x() { return x; }
 *     public int y() { return y; }
 * }
 * }</pre>
 *
 * @see Immutable
 * @see java.lang.Record
 * @see com.landawn.abacus.util.Beans#isRecordClass(Class)
 */
@Test
@Documented
@Target(value = { ElementType.TYPE })
@Retention(RUNTIME)
public @interface Record {

}
