/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

import com.landawn.abacus.util.MapEntity;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link MapEntity} objects, providing conversion between {@code MapEntity}
 * instances and their JSON string representation.
 *
 * <p>Serialization uses the configured JSON parser. The serialization type is
 * {@link SerializationType#MAP_ENTITY}, and direct byte-level serialization is not supported
 * ({@link #isSerializable()} returns {@code false}).
 *
 * @see MapEntity
 */
@SuppressWarnings("java:S2160")
public class MapEntityType extends AbstractType<MapEntity> {

    public static final String MAP_ENTITY = MapEntity.class.getSimpleName();

    private final Class<MapEntity> typeClass;

    MapEntityType() {
        super(MAP_ENTITY);

        typeClass = MapEntity.class;
    }

    /**
     * Returns the {@link Class} object representing the {@link MapEntity} type.
     *
     * @return {@code MapEntity.class}
     */
    @Override
    public Class<MapEntity> javaType() {
        return typeClass;
    }

    /**
     * Indicates whether this type represents a {@link MapEntity}.
     * Always returns {@code true} for {@code MapEntityType}.
     *
     * @return {@code true}
     */
    @Override
    public boolean isMapEntity() {
        return true;
    }

    /**
     * Indicates whether instances of this type support direct byte-level serialization.
     * {@link MapEntity} objects are not directly serializable through this type handler;
     * they are converted to/from JSON string form instead.
     *
     * @return {@code false}
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for {@link MapEntity}.
     * This value informs the serialization framework how to handle {@code MapEntity} instances.
     *
     * @return {@link SerializationType#MAP_ENTITY}
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.MAP_ENTITY;
    }

    /**
     * Converts a {@link MapEntity} object to its JSON string representation.
     * The {@code MapEntity} is serialized using the configured JSON parser with default settings.
     *
     * @param x the {@code MapEntity} object to convert, may be {@code null}
     * @return the JSON string representation of the {@code MapEntity},
     *         or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final MapEntity x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string to create a {@link MapEntity} object.
     * The string must be a valid JSON object representation that can be deserialized into a {@code MapEntity}.
     *
     * @param str the JSON string to parse, may be {@code null} or blank
     * @return the parsed {@code MapEntity} object, or {@code null} if the input is {@code null} or blank
     */
    @Override
    public MapEntity valueOf(final String str) {
        return Strings.isBlank(str) ? null : Utils.jsonParser.deserialize(str, Utils.jdc, MapEntity.class);
    }
}
